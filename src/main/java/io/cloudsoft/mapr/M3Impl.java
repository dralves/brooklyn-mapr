package io.cloudsoft.mapr;

import io.cloudsoft.mapr.m3.AbstractM3Node;
import io.cloudsoft.mapr.m3.MasterNode;
import io.cloudsoft.mapr.m3.WorkerNode;
import io.cloudsoft.mapr.m3.ZookeeperWorkerNode;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.basic.SensorPropagatingEnricher;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.trait.StartableMethods;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.location.Location;
import brooklyn.policy.autoscaling.AutoScalerPolicy;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class M3Impl extends AbstractEntity implements M3 {

   public static final Logger log = LoggerFactory.getLogger(M3Impl.class);

   MasterNode master;
   ZookeeperWorkerNode zk1;
   ZookeeperWorkerNode zk2;
   DynamicCluster workers;

   public M3Impl() {
      super();
   }

   public M3Impl(Entity parent) {
      super(parent);
   }

   public M3Impl(Map flags, Entity parent) {
      super(flags, parent);
   }

   @Override
   public void init() {
      // The DB master
      master = (MasterNode) addChild(getEntityManager().createEntity(BasicEntitySpec.newInstance(MasterNode.class)
              .configure("name", "node1 (master)")));

      // The zookeeper nodes
      zk1 = (ZookeeperWorkerNode) addChild(getEntityManager().createEntity(BasicEntitySpec.newInstance(ZookeeperWorkerNode.class)
              .configure("name", "node2")));

      zk2 = (ZookeeperWorkerNode) addChild(getEntityManager().createEntity(BasicEntitySpec.newInstance(ZookeeperWorkerNode.class)
              .configure("name", "node3")));

      // The dynamic cluster
      workers = (DynamicCluster) addChild(getEntityManager().createEntity(BasicEntitySpec.newInstance(DynamicCluster.class)
              .configure(Cluster.INITIAL_SIZE, 2)
              .configure("memberSpec", BasicEntitySpec.newInstance(WorkerNode.class))));

      workers.addPolicy(AutoScalerPolicy.builder()
              .metric(MasterNode.CLUSTER_USED_DFS_PERCENT)
              .entityWithMetric(master)
              .sizeRange(2, 5)
              .metricRange(20.0, 80.0)
              .build());


      setConfig(MASTER_UP, DependentConfiguration.attributeWhenReady(master, MasterNode.SERVICE_UP));
      setConfig(MASTER_HOSTNAME, DependentConfiguration.attributeWhenReady(master, MasterNode.SUBNET_HOSTNAME));
       
      Iterable<Entity> zookeeperNodes = Iterables.filter(getChildren(), new Predicate<Entity>() {
         @Override
         public boolean apply(@Nullable Entity input) {
            return AbstractM3Node.class.isAssignableFrom(input.getClass()) && ((AbstractM3Node) input).isZookeeper();
         }
      });

      setConfig(ZOOKEEPER_HOSTNAMES, DependentConfiguration.listAttributesWhenReady(AbstractM3Node.SUBNET_HOSTNAME,
              zookeeperNodes));
      setConfig(ZOOKEEPER_READY, DependentConfiguration.listAttributesWhenReady(AbstractM3Node.ZOOKEEPER_UP,
              zookeeperNodes));

      SensorPropagatingEnricher.newInstanceListeningTo(master, MasterNode.MAPR_URL).addToEntityAndEmitAll(this);
   }

   @Override
   public void start(Collection<? extends Location> locations) { StartableMethods.start(this, locations); }

   @Override
   public void stop() { StartableMethods.stop(this); }

   @Override
   public void restart() { StartableMethods.restart(this); }

}
