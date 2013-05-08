package io.cloudsoft.mapr.m3;

import brooklyn.config.render.RendererHints;
import brooklyn.entity.Entity;
import brooklyn.event.feed.function.FunctionFeed;
import brooklyn.event.feed.function.FunctionPollConfig;
import brooklyn.location.Location;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.mysql.jdbc.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static brooklyn.util.exceptions.Exceptions.propagate;
import static com.google.common.collect.ImmutableList.of;

public class MasterNodeImpl extends AbstractM3NodeImpl implements MasterNode {

  private static final Logger log = LoggerFactory.getLogger(MasterNodeImpl.class);

  static {
    RendererHints.register(MAPR_URL, new RendererHints.NamedActionWithUrl("Open"));
  }

  private FunctionFeed feed;

  public MasterNodeImpl() {
    super();
  }

  public MasterNodeImpl(Entity owner) {
    super(owner);
  }

  public MasterNodeImpl(Map properties, Entity owner) {
    super(properties, owner);
  }

  private Connection connection;
  private PreparedStatement usedSpaceStatement;
  private PreparedStatement availableSpaceStatement;

  public boolean isZookeeper() { return true; }

  public List<String> getAptPackagesToInstall() {
    return ImmutableList.<String>builder().add("mapr-cldb", "mapr-jobtracker", "mapr-nfs", "mapr-webserver",
      "mapr-zookeeper").addAll(super.getAptPackagesToInstall()).build();
  }

  public void setupAdminUser() {
    getDriver().exec(of("sudo /opt/mapr/bin/maprcli acl edit -type cluster -user " + getUser() +
      ":fc"));
  }

  public void setupMySql() {
    log.info("Master node setting up mysql metrics storage...");
    getDriver().exec(of(
      // mysql needs this export in order not to require a password
      "export DEBIAN_FRONTEND=noninteractive;sudo -E -n -s -- apt-get install -y --allow-unauthenticated " +
        "mysql-server",
      "sudo sed -i s/127.0.0.1/0.0.0.0/ /etc/mysql/my.cnf",
      "mysqladmin -u root password " + getPassword(),
      "sudo /etc/init.d/mysql restart",
      "echo \"GRANT ALL ON *.* TO '" + getUser() + "'@'%' IDENTIFIED BY '" + getPassword() + "';\" | sudo tee" +
        " -a /tmp/grant-all-cmd.sql > /dev/null",
      "mysql -u root -p" + getPassword() + " < /tmp/grant-all-cmd.sql",
      "mysql -u root -p" + getPassword() + " < /opt/mapr/bin/setup.sql"));
    log.info("Mysql metrics storage setup.");
  }

  public void startMasterServices() {
    // start the services -- no longer needed in v2
//    if (...VERSION.startsWith("v1."))
//    getDriver().exec(of("sudo /opt/mapr/bin/maprcli node services -nodes " + getAttribute(SUBNET_HOSTNAME) + " -nfs start"));
  }

  public void startServices() {
    getDriver().startWarden();
    setupAdminUser();
    startMasterServices();

    // TODO this should happen on all nodes
    // (but isn't needed except for metrics)
    // since v2 seems this must be done after warden is started?
    getDriver().setupAdminUserMapr(getUser(), getPassword());

    // not sure this sleep is necessary
    try {
      Thread.sleep(10 * 1000);
    } catch (InterruptedException e) {
      propagate(e);
    }
    setAttribute(MAPR_URL, "https://" + getAttribute(HOSTNAME) + ":8443");

    try {
      Class.forName(Driver.class.getName()).newInstance();
      connection = DriverManager.getConnection("jdbc:mysql://" + getAttribute(HOSTNAME) + ":3306/metrics", getUser(),
        getPassword());
      usedSpaceStatement = connection.prepareStatement(usedSpaceQuery);
      availableSpaceStatement = connection.prepareStatement(availSpaceQuery);
    } catch (Exception e) {
      propagate(e);
    }

    this.feed = FunctionFeed.builder()
      .entity(this)
      .poll(new FunctionPollConfig<Double, Double>(CLUSTER_USED_DFS_PERCENT)
        .period(5, TimeUnit.SECONDS)
        .callable(new Callable<Double>() {
          @Override
          public Double call() throws Exception {
            // creating a new sql per query isnt the way to go

            ResultSet usedSpaceResult = usedSpaceStatement.executeQuery();
            ResultSet availableSpaceResult = availableSpaceStatement.executeQuery();

            int sumUsed = 0;
            while (usedSpaceResult.next()) {
              sumUsed += usedSpaceResult.getInt("space");
            }

            int sumAvail = 0;
            while (availableSpaceResult.next()) {
              sumUsed += availableSpaceResult.getInt("space");
            }

            log.info("current dfs usage: " + 100 * sumUsed / (sumUsed + sumAvail));
            return (100.0 * sumUsed) / (sumUsed + sumAvail);
          }
        })
        .onSuccess(Functions.<Double>identity())
        .onError(new Function<Exception, Double>() {
          @Nullable
          @Override
          public Double apply(@Nullable Exception input) {
            LOG.error("Error fetching dfs usage.", input);
            return .5;
          }
        })
      ).build();
  }

  public void start(Collection<? extends Location> locations) {
    if (getPassword() == null)
      throw new IllegalArgumentException("configuration " + MAPR_PASSWORD.getName() + " must be specified");
    super.start(locations);
  }

  public boolean isMaster() { return true; }

  @Override
  public void stop() {
    if (this.feed != null) {
      this.feed.stop();
    }
    try {
      if (connection != null) connection.close();
    } catch (SQLException e) {
      propagate(e);
    }
    super.stop();
  }
}
