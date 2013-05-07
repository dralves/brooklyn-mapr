package io.cloudsoft.mapr.m3;

import io.cloudsoft.mapr.M3;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;

import com.google.common.collect.ImmutableList;

public class ZookeeperWorkerNodeImpl extends AbstractM3NodeImpl implements ZookeeperWorkerNode {

   private static final Logger log = LoggerFactory.getLogger(ZookeeperWorkerNodeImpl.class);
    
   public ZookeeperWorkerNodeImpl() {
      super();
   }

   public ZookeeperWorkerNodeImpl(Entity owner) {
      super(owner);
   }

   public ZookeeperWorkerNodeImpl(Map properties, Entity owner) {
      super(properties, owner);
   }

   public List<String> getAptPackagesToInstall() {
      return ImmutableList.<String>builder().add("mapr-zookeeper").addAll(super.getAptPackagesToInstall()).build();
   }

   public boolean isZookeeper() { return true; }

   public void startServices() {
      log.info("ZookeeperWorkerNode node {} waiting for master", this);
      getConfig(M3.MASTER_UP);
      log.info("ZookeeperWorkerNode node {} detected master up", this);
   }

}
