package io.cloudsoft.mapr.m3

import brooklyn.config.render.RendererHints
import brooklyn.event.basic.BasicAttributeSensor
import brooklyn.location.Location
import groovy.transform.InheritConstructors
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@InheritConstructors
class MasterNode extends AbstractM3Node {

    public static final Logger log = LoggerFactory.getLogger(MasterNode.class);

    static String usedSpaceQuery = """
select a.EVENT_TIME as \"timestamp\", a.NODE_ID as \"node\", a.M_VALUE as \"space\" from METRIC_TRANSACTION a
    inner join
        (select e.NODE_ID, e.M_NAME, max(e.EVENT_TIME) as max_time from METRIC_TRANSACTION e where e.M_NAME = \"SRVUSEDMB\" group by e.NODE_ID) b
    on a.EVENT_TIME = b.max_time and a.M_NAME = b.M_NAME group by a.NODE_ID order by a.NODE_ID;
""";
    static String availSpaceQuery = """
select a.EVENT_TIME as \"timestamp\", a.NODE_ID as \"node\", a.M_VALUE as \"space\" from METRIC_TRANSACTION a
    inner join
        (select e.NODE_ID, e.M_NAME, max(e.EVENT_TIME) as max_time from METRIC_TRANSACTION e where e.M_NAME = \"SRVAVAILMB\" group by e.NODE_ID) b
    on a.EVENT_TIME = b.max_time and a.M_NAME = b.M_NAME group by a.NODE_ID order by a.NODE_ID;
""";

    public static final BasicAttributeSensor<Double> CLUSTER_USED_DFS_PERCENT =
        [Double, "cluster.used.dfs.percent", "The percentage o the cluster DFS that is currently being used."];


    public static final BasicAttributeSensor<String> MAPR_URL = [String, "mapr.url", "URL where MapR can be accessed"];

    static {
        RendererHints.register(MAPR_URL, new RendererHints.NamedActionWithUrl("Open"));
    }

    public boolean isZookeeper() { return true; }

    public List<String> getAptPackagesToInstall() {
        ["mapr-cldb", "mapr-jobtracker", "mapr-nfs", "mapr-webserver", "mapr-zookeeper", "mysql-server"] + super.getAptPackagesToInstall();
    }

    public void setupAdminUser() {
        driver.exec(["sudo /opt/mapr/bin/maprcli acl edit -type cluster -user ${user}:fc"]);
    }

    public void setupMySql() {
        driver.exec([
                "sudo sed -i s/127.0.0.1/0.0.0.0/ /etc/mysql/my.cnf",
                "mysqladmin -u root password ${password}",
                "sudo /etc/init.d/mysql restart",
                "echo \"GRANT ALL ON *.* TO '${user}'@'%' IDENTIFIED BY '${password}';\" | sudo tee -a /tmp/grant-all-cmd.sql > /dev/null",
                "mysql -u root -p${password} < /tmp/grant-all-cmd.sql",
                "mysql -u root -p${password} < /opt/mapr/bin/setup.sql"]);
    }

    public void startMasterServices() {
        // start the services
        driver.exec(["sudo /opt/mapr/bin/maprcli node services -nodes ${getAttribute(SUBNET_HOSTNAME)} -nfs start"]);
    }

    public void runMaprPhase2() {
        driver.startWarden();
        setupAdminUser();
        startMasterServices();

        // not sure this sleep is necessary, but seems safer...
        Thread.sleep(10 * 1000);
        setAttribute(MAPR_URL, "https://${getAttribute(HOSTNAME)}:8443")

//        FunctionSensorAdapter dfsUsageSensor = sensorRegistry.register(new FunctionSensorAdapter(
//                MutableMap.of("period", 1 * 5000),
//                new Callable<Double>() {
//                    public Double call() {
//                        // creating a new sql per query isnt the way to go
//                        def sql = Sql.newInstance("jdbc:mysql://${getAttribute(HOSTNAME)}:3306/metrics", getUser(), getPassword());
//
//                        List<GroovyRowResult> usedSpace = []
//                        sql.eachRow(usedSpaceQuery) {
//                            usedSpace << it.toRowResult()
//                        }
//                        List<GroovyRowResult> availSpace = []
//                        sql.eachRow(availSpaceQuery) {
//                            availSpace << it.toRowResult()
//                        }
//
//                        int sumUsed = 0;
//                        int sumAvail = 0;
//                        for (int i = 0; i < usedSpace.size(); i++) {
//                            def used = usedSpace[i];
//                            def avail = availSpace[i];
//                            sumUsed += used.get("space");
//                            sumAvail += avail.get("space");
//                        }
//                        log.info("current dfs usage: " + 100 * sumUsed / (sumUsed + sumAvail));
//                        return 100 * sumUsed / (sumUsed + sumAvail);
//                    }
//                }));
//        dfsUsageSensor.poll(CLUSTER_USED_DFS_PERCENT);


    }

    public void start(Collection<? extends Location> locations) {
        if (!getPassword())
            throw new IllegalArgumentException("configuration " + MAPR_PASSWORD.getName() + " must be specified");
        super.start(locations);
    }

    public boolean isMaster() { return true; }


}
