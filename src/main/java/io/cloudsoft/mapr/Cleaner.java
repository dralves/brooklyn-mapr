package io.cloudsoft.mapr;

import com.google.common.collect.Sets;
import org.jclouds.Context;
import org.jclouds.ContextBuilder;
import org.jclouds.googlecompute.GoogleComputeApi;
import org.jclouds.googlecompute.GoogleComputeApiMetadata;
import org.jclouds.googlecompute.domain.Firewall;
import org.jclouds.googlecompute.domain.Instance;
import org.jclouds.googlecompute.domain.Network;
import org.jclouds.googlecompute.domain.Operation;
import org.jclouds.googlecompute.features.FirewallApi;
import org.jclouds.googlecompute.features.InstanceApi;
import org.jclouds.googlecompute.features.NetworkApi;
import org.jclouds.googlecompute.predicates.OperationDonePredicate;
import org.jclouds.util.Predicates2;
import org.jclouds.util.Strings2;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author David Alves
 */
public class Cleaner {

  public static void main(String[] args) throws IOException, InterruptedException {

    String projectName = "590421487852";

    Properties properties = new Properties();
    properties.setProperty("google-compute.credential", Strings2.toStringAndClose(new FileInputStream("cloudsoft-gce-pk.pem")));
    properties.setProperty("google-compute.identity", projectName + "@developer.gserviceaccount.com");

    Context context = ContextBuilder
      .newBuilder(new GoogleComputeApiMetadata())
      .overrides(properties)
      .build();

    final GoogleComputeApi api = context
      .getUtils()
      .getInjector()
      .getInstance(GoogleComputeApi.class);

    final OperationDonePredicate predicate = context.getUtils().getInjector().getInstance(OperationDonePredicate.class);

    final InstanceApi instanceApi = api.getInstanceApiForProject(projectName);
    final Set<Instance> instances = Sets.newLinkedHashSet(instanceApi.list().concat());

    ExecutorService service = Executors.newFixedThreadPool(instances.size() != 0 ? instances.size() : 1);
    for (final Instance instance : instances) {
      service.submit(new Runnable() {
        @Override
        public void run() {
          waitDone(instanceApi.delete(instance.getName()), predicate);
        }
      });
    }
    service.shutdown();
    service.awaitTermination(30, TimeUnit.SECONDS);
    System.out.println("instances deleted");

    final FirewallApi firewallApi = api.getFirewallApiForProject(projectName);
    final Set<Firewall> firewalls = Sets.newLinkedHashSet(firewallApi.list().concat());

    service = Executors.newFixedThreadPool(firewalls.size() != 0 ? firewalls.size() : 1);
    for (final Firewall firewall : firewalls) {
      service.submit(new Runnable() {
        @Override
        public void run() {
          waitDone(firewallApi.delete(firewall.getName()), predicate);
        }
      });
    }
    service.shutdown();
    service.awaitTermination(30, TimeUnit.SECONDS);
    System.out.println("firewalls deleted");

    final NetworkApi networkApi = api.getNetworkApiForProject(projectName);
    final Set<Network> networks = Sets.newLinkedHashSet(networkApi.list().concat());

    service = Executors.newFixedThreadPool(networks.size() != 0 ? networks.size() : 1);
    for (final Network network : networks) {
      service.submit(new Runnable() {
        @Override
        public void run() {
          waitDone(networkApi.delete(network.getName()), predicate);
        }
      });
    }
    service.shutdown();
    service.awaitTermination(30, TimeUnit.SECONDS);
    System.out.println("networks deleted");

  }

  private static void waitDone(Operation operation, OperationDonePredicate predicate) {
    AtomicReference<Operation> ref = new AtomicReference<Operation>();
    Predicates2.retry(predicate, 10000);
  }
}
