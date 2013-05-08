package io.cloudsoft.mapr;

import brooklyn.enricher.basic.SensorPropagatingEnricher;
import brooklyn.entity.Application;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.util.CommandLineUtil;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.cloudsoft.mapr.m3.AbstractM3Node;
import io.cloudsoft.mapr.m3.M3Disks;
import io.cloudsoft.mapr.m3.MasterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * starts an M3 cluster in AWS. login as username 'ubuntu', password 'm4pr'.
 */
public class MyM3App extends AbstractApplication {

  private static final Logger log = LoggerFactory.getLogger(MyM3App.class);

  final static String DEFAULT_LOCATION =
    "google-compute-engine";
//        "cloudstack-citrix";
//        "aws-ec2-us-east-1-centos";

  @Override
  public void init() {
    M3 m3 = addChild(BasicEntitySpec.newInstance(M3.class)

      // EDIT to choose your own password
      // (you can also specify a MAPR_USERNAME; the default is 'mapr')
      .configure(MasterNode.MAPR_PASSWORD, "m4pr")

        // disks can be set specifically on any entity;
        // setting on the root will provide a default which is inherited everywhere
        // this default is an on-disk flat-file
      .configure(AbstractM3Node.DISK_SETUP_SPEC, M3Disks.builder().
        disks(
          "/mnt/mapr-storagefile1",
          "/mnt/mapr-storagefile2").
        commands(
          "sudo truncate -s 10G /mnt/mapr-storagefile1",
          "sudo truncate -s 10G /mnt/mapr-storagefile2").
        build()));

    // show URL at top level
    SensorPropagatingEnricher.newInstanceListeningTo(m3, MasterNode.MAPR_URL).addToEntityAndEmitAll(this);
  }

  public static void main(String[] argv) throws IOException {

    System.setProperty("jclouds.ssh.max-retries", 20 + "");
    System.setProperty("jclouds.so-timeout", 120 * 1000 + "");
    System.setProperty("jclouds.connection-timeout", 120 * 1000 + "");


    List<String> args = Lists.newArrayList(argv);
    String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
    String location = CommandLineUtil.getCommandLineOption(args, "--location", DEFAULT_LOCATION);

    BrooklynLauncher launcher = BrooklynLauncher.newInstance()
      .application(EntitySpecs.appSpec(MyM3App.class).displayName("MapR"))
      .webconsolePort(port)
      .location(location)
      .start();

    Entities.dumpInfo(launcher.getApplications());

    Application app = Iterables.getOnlyElement(launcher.getApplications());
    log.info("RUNNING MapR at " + app.getAttribute(MasterNode.MAPR_URL));
  }
}
