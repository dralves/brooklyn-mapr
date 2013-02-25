package io.cloudsoft.mapr.m3;

import io.cloudsoft.mapr.M3;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;

import com.google.common.collect.ImmutableList;

public class WorkerNodeImpl extends AbstractM3NodeImpl implements WorkerNode {

   private static final Logger log = LoggerFactory.getLogger(WorkerNodeImpl.class);
   
   public WorkerNodeImpl() {
      super();
   }

   public WorkerNodeImpl(Entity owner) {
      super(owner);
   }

   public WorkerNodeImpl(Map properties, Entity owner) {
      super(properties, owner);
   }

   public List<String> getAptPackagesToInstall() {
      return ImmutableList.<String>builder().add("mapr-fileserver", "mapr-tasktracker").addAll(super
              .getAptPackagesToInstall()).build();
   }

   public void startServices() {
      log.info("MapR node {} waiting for master", this);
      getConfig(M3.MASTER_UP);
      log.info("MapR node {} detected master up, proceeding to start warden", this);
      getDriver().startWarden();
   }

}
