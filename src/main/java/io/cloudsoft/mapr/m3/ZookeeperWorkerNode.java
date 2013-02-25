package io.cloudsoft.mapr.m3;

import brooklyn.entity.proxying.ImplementedBy;

@ImplementedBy(ZookeeperWorkerNodeImpl.class)
public interface ZookeeperWorkerNode extends AbstractM3Node {
}
