package io.cloudsoft.mapr;

import brooklyn.enricher.basic.SensorPropagatingEnricher;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.BasicConfigurableEntityFactory;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Startable;
import brooklyn.entity.trait.StartableMethods;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.location.Location;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.cloudsoft.mapr.m3.AbstractM3Node;
import io.cloudsoft.mapr.m3.MasterNode;
import io.cloudsoft.mapr.m3.WorkerNode;
import io.cloudsoft.mapr.m3.ZookeeperWorkerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Maps.newLinkedHashMap;

@ImplementedBy(M3Impl.class)
public interface M3 extends Entity, Startable {

   public static BasicConfigKey<String> MASTER_HOSTNAME = new BasicConfigKey<String>(
           String.class, "mapr.master.hostname", "");

   /**
    * hostnames of all machines expected to run zookeeper
    */
   public static BasicConfigKey<List<String>> ZOOKEEPER_HOSTNAMES = new BasicConfigKey(
           List.class, "mapr.zk.hostnames", "");
   /**
    * configuration is set when all expected zookeepers have started the zookeeper process
    */
   public static BasicConfigKey<List<Boolean>> ZOOKEEPER_READY = new BasicConfigKey(
           List.class, "mapr.zk.ready", "");

   /**
    * configuration is set when the master node has come up (license approved etc)
    */
   public static BasicConfigKey<Boolean> MASTER_UP = new BasicConfigKey<Boolean>(
           Boolean.class, "mapr.master.serviceUp", "");
}
