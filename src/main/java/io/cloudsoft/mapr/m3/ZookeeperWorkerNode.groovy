package io.cloudsoft.mapr.m3

import groovy.transform.InheritConstructors
import io.cloudsoft.mapr.M3;

@InheritConstructors
class ZookeeperWorkerNode extends AbstractM3Node {

    public List<String> getAptPackagesToInstall() {
        return ["mapr-zookeeper"] + super.getAptPackagesToInstall();
    }

    public boolean isZookeeper() { return true; }

    public void runMaprPhase2() {
        log.info("ZookeeperWorkerNode node {} waiting for master", this);
        getConfig(M3.MASTER_UP);
        log.info("ZookeeperWorkerNode node {} detected master up", this);
    }
    
}
