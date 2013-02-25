package io.cloudsoft.mapr.m3;

import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.trait.Startable;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicConfigKey;

public interface AbstractM3Node extends SoftwareProcess, Startable {

   public static BasicConfigKey<DiskSetupSpec> DISK_SETUP_SPEC = new BasicConfigKey<DiskSetupSpec>(DiskSetupSpec
           .class, "mapr.node.disk.setup");
   public static final BasicAttributeSensor<Boolean> ZOOKEEPER_UP = new BasicAttributeSensor<Boolean>(Boolean.class,
           "mapr.zookeeper.serviceUp", "whether zookeeper has been started");

   public static BasicConfigKey<String> MAPR_USERNAME = new BasicConfigKey<String>(String.class, "mapr.username",
           "initial user to create for mapr", "mapr");
   public static BasicConfigKey<String> MAPR_PASSWORD = new BasicConfigKey<String>(String.class, "mapr.password",
           "initial password for initial user");

   public static final BasicAttributeSensor<String> SUBNET_HOSTNAME = new BasicAttributeSensor<String>(String.class,
           "machine.subnet.hostname", "internally resolvable hostname");

   // TODO config param?  note, if this is not 'ubuntu', we have to create the user; see jclouds AdminAccess
   public String getUser();

   public String getPassword();

   public void configureMetrics(String hostname);

   public void setupMapRUser();

   public boolean isZookeeper();

   public boolean isMaster();

   public void startServices();
}
