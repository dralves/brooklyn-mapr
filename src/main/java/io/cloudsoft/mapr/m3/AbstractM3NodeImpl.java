package io.cloudsoft.mapr.m3;

import java.util.List;
import java.util.Map;

import org.jclouds.compute.domain.OsFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Lifecycle;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.jclouds.templates.PortableTemplateBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public abstract class AbstractM3NodeImpl extends SoftwareProcessImpl implements AbstractM3Node {

   public static final Logger log = LoggerFactory.getLogger(AbstractM3Node.class);

   public AbstractM3NodeImpl() {
      this(ImmutableMap.of(), null);
   }

   public AbstractM3NodeImpl(Entity owner) {
      this(ImmutableMap.of(), owner);
   }

   public AbstractM3NodeImpl(Map properties, Entity owner) {
      super(properties, owner);
      setAttribute(SERVICE_UP, false);
      setAttribute(SERVICE_STATE, Lifecycle.CREATED);
   }

   // TODO config param?  note, if this is not 'ubuntu', we have to create the user; see jclouds AdminAccess
   @Override
   public String getUser() { return getConfig(MAPR_USERNAME); }

   @Override
   public String getPassword() { return getConfig(MAPR_PASSWORD); }

   @Override
   public void configureMetrics(String hostname) {
      getDriver().exec(ImmutableList.of("sudo /opt/mapr/server/configure.sh -R -d " + hostname + ":3306 -du " +
              getUser()
              + " -dp " +
              getPassword() + " -ds metrics"));
   }

   @Override
   public void setupMapRUser() {
      getDriver().exec(ImmutableList.of(
              "sudo adduser " + getUser() + " < /dev/null || true",
              "echo \"" + getPassword() + "\n" + getPassword() + "\" | sudo passwd " + getUser() + ""));
   }

   @Override
   public boolean isZookeeper() { return false; }

   @Override
   public boolean isMaster() { return false; }

   public List<String> getAptPackagesToInstall() {
      return ImmutableList.of("mapr-metrics");
   }

   @Override
   public Class<? extends M3NodeDriver> getDriverInterface() {
      return M3NodeDriver.class;
   }

   @Override
   public M3NodeDriver getDriver() {
      return (M3NodeDriver) super.getDriver();
   }

   protected Map<String, Object> getProvisioningFlags(MachineProvisioningLocation location) {
      return obtainProvisioningFlags(location);
   }

   protected Map<String, Object> obtainProvisioningFlags(MachineProvisioningLocation location) {
      Map flags = super.obtainProvisioningFlags(location); 
      Iterable<Integer> superInboundPorts = (Iterable<Integer>) flags.get("inboundPorts");
      
      flags.put("templateBuilder", new PortableTemplateBuilder().
              osFamily(OsFamily.UBUNTU).osVersionMatches("11.04").os64Bit(true).
              minRam(2560));

      flags.put("userName", "ubuntu");
      
      // from: http://www.mapr.com/doc/display/MapR/Ports+Used+by+MapR
      // 3888 discovered also to be needed; 2888 included for good measure
      flags.put("inboundPorts", ImmutableSet.<Integer>builder()
              .addAll(ImmutableList.of(22, 2048, 3306, 5660, 5181, 7221, 7222, 8080, 8443, 9001, 9997, 9998, 50030, 50060, 60000, 2888, 3888))
              .addAll(superInboundPorts == null ? ImmutableList.<Integer>of() : superInboundPorts)
              .build());
      
      return flags;
   }

   public abstract void startServices();

}
