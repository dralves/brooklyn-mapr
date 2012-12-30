package io.cloudsoft.mapr.m3

import groovy.transform.InheritConstructors
import io.cloudsoft.mapr.M3
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@InheritConstructors
class WorkerNode extends AbstractM3Node {

    public List<String> getAptPackagesToInstall() {
        return ["mapr-fileserver", "mapr-tasktracker"] + super.getAptPackagesToInstall();
    }

    public static final Logger log = LoggerFactory.getLogger(WorkerNode.class);

    public void runMaprPhase2() {
        log.info("MapR node {} waiting for master", this);
        getConfig(M3.MASTER_UP);
        log.info("MapR node {} detected master up, proceeding to start warden", this);
        driver.startWarden();
    }

}
