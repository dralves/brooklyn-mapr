package io.cloudsoft.mapr.m3;

import brooklyn.entity.proxying.ImplementedBy;

@ImplementedBy(WorkerNodeImpl.class)
public interface WorkerNode extends AbstractM3Node {
}
