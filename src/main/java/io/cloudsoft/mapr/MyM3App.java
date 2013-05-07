package io.cloudsoft.mapr;

import io.cloudsoft.mapr.m3.AbstractM3Node;
import io.cloudsoft.mapr.m3.M3Disks;
import io.cloudsoft.mapr.m3.MasterNode;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.basic.SensorPropagatingEnricher;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.launcher.BrooklynServerDetails;
import brooklyn.location.Location;
import brooklyn.util.CommandLineUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/** starts an M3 cluster in AWS. login as username 'ubuntu', password 'm4pr'. */
public class MyM3App extends ApplicationBuilder {

    private static final Logger log = LoggerFactory.getLogger(MyM3App.class);

    final static String DEFAULT_LOCATION =
        "aws-ec2:us-east-1";
//        "cloudstack-citrix";
//        "aws-ec2-us-east-1-centos";

    @Override
    public void doBuild() {
        M3 m3 = createChild(BasicEntitySpec.newInstance(M3.class)

            // EDIT to choose your own password
            // (you can also specify a MAPR_USERNAME; the default is 'mapr')
            .configure(MasterNode.MAPR_PASSWORD, "m4pr")
            
            // disks can be set specifically on any entity; 
            // setting on the root will provide a default which is inherited everywhere 
            // this default is an on-disk flat-file
            .configure(AbstractM3Node.DISK_SETUP_SPEC, M3Disks.builder().
                    disks(
                        "/mnt/mapr-storagefile1", 
                        "/mnt/mapr-storagefile2").
                    commands(
                        "sudo truncate -s 20G /mnt/mapr-storagefile1",
                        "sudo truncate -s 10G /mnt/mapr-storagefile2").
                    build() ));
        
        // show URL at top level
        SensorPropagatingEnricher.newInstanceListeningTo(m3, MasterNode.MAPR_URL).addToEntityAndEmitAll(getApp());
    }

    public static void main(String[] argv) {
        System.setProperty("jclouds.ssh.max-retries", 20 + "");
        System.setProperty("jclouds.so-timeout", 120 * 1000 + "");
        System.setProperty("jclouds.connection-timeout", 120 * 1000 + "");

        List<String> args = Lists.newArrayList(argv);
        String port =  CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", DEFAULT_LOCATION);

        BrooklynServerDetails server = BrooklynLauncher.newLauncher()
                .webconsolePort(port)
                .launch();

        Location loc = server.getManagementContext().getLocationRegistry().resolve(location);

        StartableApplication app = new MyM3App()
                .manage(server.getManagementContext());
        
        app.start(ImmutableList.of(loc));
        
        log.info("RUNNING MapR at "+app.getAttribute(MasterNode.MAPR_URL));
    }
}
