package io.cloudsoft.mapr.m3;

import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicAttributeSensor;

@ImplementedBy(MasterNodeImpl.class)
public interface MasterNode extends AbstractM3Node {

   static String usedSpaceQuery =
           "select a.EVENT_TIME as \"timestamp\", a.NODE_ID as \"node\", " +
                   "a.M_VALUE as \"space\" from METRIC_TRANSACTION a" +
                   "inner join" +
                   "(select e.NODE_ID, e.M_NAME, max(e.EVENT_TIME) as max_time from METRIC_TRANSACTION e where e" +
                   ".M_NAME = \"SRVUSEDMB\" group by e.NODE_ID) b" +
                   "on a.EVENT_TIME = b.max_time and a.M_NAME = b.M_NAME group by a.NODE_ID order by a.NODE_ID;";

   static String availSpaceQuery =
           "select a.EVENT_TIME as \"timestamp\", a.NODE_ID as \"node\", " +
                   "a.M_VALUE as \"space\" from METRIC_TRANSACTION a" +
                   "inner join" +
                   "(select e.NODE_ID, e.M_NAME, max(e.EVENT_TIME) as max_time from METRIC_TRANSACTION e where e" +
                   ".M_NAME = \"SRVAVAILMB\" group by e.NODE_ID) b" +
                   "on a.EVENT_TIME = b.max_time and a.M_NAME = b.M_NAME group by a.NODE_ID order by a.NODE_ID;";

   public static final BasicAttributeSensor<Double> CLUSTER_USED_DFS_PERCENT =
           new BasicAttributeSensor<Double>(Double.class, "cluster.used.dfs.percent",
                   "The percentage o the cluster DFS that is currently being used.");


   public static final BasicAttributeSensor<String> MAPR_URL =
           new BasicAttributeSensor<String>(String.class, "mapr.url", "URL where MapR can be accessed");

   public void startMasterServices();
}
