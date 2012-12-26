package io.cloudsoft.mapr

import brooklyn.enricher.basic.SensorPropagatingEnricher
import brooklyn.entity.basic.AbstractEntity
import brooklyn.entity.basic.BasicConfigurableEntityFactory
import brooklyn.entity.group.Cluster
import brooklyn.entity.group.DynamicCluster
import brooklyn.entity.trait.Startable
import brooklyn.entity.trait.StartableMethods
import brooklyn.event.basic.BasicConfigKey
import brooklyn.event.basic.DependentConfiguration
import brooklyn.location.Location
import groovy.transform.InheritConstructors
import io.cloudsoft.mapr.m3.AbstractM3Node
import io.cloudsoft.mapr.m3.MasterNode
import io.cloudsoft.mapr.m3.WorkerNode
import io.cloudsoft.mapr.m3.ZookeeperWorkerNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@InheritConstructors
public class M3 extends AbstractEntity implements Startable {

    public static final Logger log = LoggerFactory.getLogger(M3.class);
    
    public static BasicConfigKey<String> MASTER_HOSTNAME = [ String, "mapr.master.hostname", "" ];
    
    /** hostnames of all machines expected to run zookeeper */
    public static BasicConfigKey<List<String>> ZOOKEEPER_HOSTNAMES = [ List, "mapr.zk.hostnames", "" ];
    /** configuration is set when all expected zookeepers have started the zookeeper process */
    public static BasicConfigKey<List<Boolean>> ZOOKEEPER_READY = [ List, "mapr.zk.ready", "" ];
    
    /** configuration is set when the master node has come up (license approved etc) */
    public static BasicConfigKey<Boolean> MASTER_UP = [ Boolean, "mapr.master.serviceUp", "" ];

    // The DB master
    MasterNode master = new MasterNode(this, name: "node1 (master)");

    // The zookeeper nodes
    ZookeeperWorkerNode zk1 = new ZookeeperWorkerNode(this, name: "node2");
    ZookeeperWorkerNode zk2 = new ZookeeperWorkerNode(this, name: "node3");

    // The Dynamic cluster
    DynamicCluster workers = new DynamicCluster(this, factory: new BasicConfigurableEntityFactory(WorkerNode),
            initialSize: 2);

    {

//        workers.addPolicy(AutoScalerPolicy.builder()
//                .metric(MasterNode.CLUSTER_USED_DFS_PERCENT)
//                .entityWithMetric(master)
//                .sizeRange(2, 5)
//                .metricRange(20.0, 80.0)
//                .build());

        Integer clusterSize = (Integer) getProperty("dynamicClusterSize");
        if (clusterSize != null) {
            workers.setConfig(Cluster.INITIAL_SIZE, clusterSize);
        }


        setConfig(MASTER_UP, DependentConfiguration.attributeWhenReady(master, MasterNode.SERVICE_UP));
        setConfig(MASTER_HOSTNAME, DependentConfiguration.attributeWhenReady(master, MasterNode.SUBNET_HOSTNAME));

        final def zookeeperNodes = ownedChildren.findAll({ (it in AbstractM3Node) && (it.isZookeeper()) });
        setConfig(ZOOKEEPER_HOSTNAMES, DependentConfiguration.listAttributesWhenReady(AbstractM3Node.SUBNET_HOSTNAME, zookeeperNodes));
        setConfig(ZOOKEEPER_READY, DependentConfiguration.listAttributesWhenReady(AbstractM3Node.ZOOKEEPER_UP, zookeeperNodes));

        SensorPropagatingEnricher.newInstanceListeningTo(master, MasterNode.MAPR_URL).addToEntityAndEmitAll(this);
    }


    @Override
    public void start(Collection<? extends Location> locations) { StartableMethods.start(this, locations); }

    @Override
    public void stop() { StartableMethods.stop(this); }

    @Override
    public void restart() { StartableMethods.restart(this); }

}
