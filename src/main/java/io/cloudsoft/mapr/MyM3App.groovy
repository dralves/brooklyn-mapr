package io.cloudsoft.mapr

import brooklyn.enricher.basic.SensorPropagatingEnricher
import brooklyn.entity.basic.AbstractApplication
import brooklyn.launcher.BrooklynLauncher
import brooklyn.launcher.BrooklynServerDetails
import brooklyn.location.Location
import brooklyn.util.CommandLineUtil
import com.google.common.base.Charsets
import com.google.common.io.Files
import io.cloudsoft.mapr.m3.AbstractM3Node
import io.cloudsoft.mapr.m3.M3Disks
import io.cloudsoft.mapr.m3.MasterNode

import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.base.Preconditions.checkState

/** starts an M3 cluster in AWS. login as username 'ubuntu', password 'm4pr'. */
public class MyM3App extends AbstractApplication {

    final static String DEFAULT_LOCATION =
        "aws-ec2:us-east-1";
//        "cloudstack-citrix";
//        "aws-ec2-us-east-1-centos";

    M3 m3 = new M3(this);

    {
        // EDIT to choose your own password
        // (you can also specify a MAPR_USERNAME; the default is 'mapr')
        setConfig(AbstractM3Node.MAPR_PASSWORD, "m4pr");

        // disks can be set specifically on any entity; 
        // setting on the root will provide a default which is inherited everywhere 
        // this default is an on-disk flat-file
        setConfig(AbstractM3Node.DISK_SETUP_SPEC,
                M3Disks.builder().
                        disks(
                                "/mnt/mapr-storagefile1",
                                "/mnt/mapr-storagefile2").
                        commands(
                                "sudo truncate -s 20G /mnt/mapr-storagefile1",
                                "sudo truncate -s 10G /mnt/mapr-storagefile2").
                        build());

        // show URL at top level
        SensorPropagatingEnricher.newInstanceListeningTo(m3, MasterNode.MAPR_URL).addToEntityAndEmitAll(this);
    }

    // can start in AWS by running this -- or use brooklyn CLI/REST for most clouds, or programmatic/config for set of fixed IP machines
    public static void main(String[] argv) {
//        String credential = loadFileContentsFromPathAsSystemProperty("credential");
//        String pk = loadFileContentsFromPathAsSystemProperty("privateKeyFile");

//        List args = new ArrayList(Arrays.asList(argv));

        System.setProperty("jclouds.ssh.max-retries", 20 + "");
        System.setProperty("jclouds.so-timeout", 120 * 1000 + "");
        System.setProperty("jclouds.connection-timeout", 120 * 1000 + "");

        MyM3App app = new MyM3App();

//        BrooklynServerDetails server = BrooklynLauncher.newLauncher().
//                setAttribute("credential", credential).
//                setAttribute("privateKeyData", null).
//                setAttribute("privateKeyFile", null).
//                setAttribute("publicKeyFile", null).
//                setAttribute("dynamicClusterSize", Integer.parseInt(getCommandLineOption(args, "--dynamic-cluster-size", "2"))).
//                webconsolePort(getCommandLineOption(args, "--port", "8081+")).
//                managing(app).
//                launch();

        List args = new ArrayList(Arrays.asList(argv));
        BrooklynServerDetails server = BrooklynLauncher.newLauncher().
//                setAttribute("dynamicClusterSize", Integer.parseInt(getCommandLineOption(args, "--dynamic-cluster-size", "2"))).
                webconsolePort(CommandLineUtil.getCommandLineOption(args, "--port", "8081+")).
                managing(app).
                launch();


        List<Location> locations = server.getManagementContext().getLocationRegistry().resolve(args ?: [DEFAULT_LOCATION])
        app.start(locations)
    }

    private static String loadFileContentsFromPathAsSystemProperty(String property) {
        String filePath = System.getProperty(property)
        checkNotNull(property, property + " property was not set")
        File file = new File(filePath);
        checkState(file.exists(), "file did not exist: " + filePath);
        String pk = Files.toString(file, Charsets.UTF_8);
        return pk;
    }


}
