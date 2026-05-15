//package com.freshworks.core.main;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.freshworks.core.shared.concurrency.executors.HagridExecutor;
//import com.freshworks.core.shared.constants.Constants;
//import com.freshworks.core.shared.sync.SyncStatusService;
//import com.freshworks.core.shared.infra.AbstractInfra;
//import com.freshworks.core.shared.infra.AbstractQueue;
//import com.freshworks.core.shared.infra.InfraFactory;
//import com.freshworks.core.shared.telemetry.metric.AbstractMetric;
//import com.freshworks.core.shared.telemetry.metric.PrometheusMetric;
//import com.freshworks.core.processor.AssetBeanDependencyService;
//import com.freshworks.core.traverser.DagService;
//import com.freshworks.core.shared.utils.Utility;
//import com.freshworks.core.traverser.AbstractStep;
//import com.google.common.collect.ImmutableMultimap;
//import com.scalified.tree.TreeNode;
//import com.sun.net.httpserver.HttpServer;
//import lombok.Getter;
//import lombok.Setter;
//import com.freshworks.core.traverser.TraverserExecutorService;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.OutputStream;
//import java.lang.reflect.InvocationTargetException;
//import java.net.InetSocketAddress;
//import java.util.HashMap;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.Callable;
//
//
//import static com.google.common.base.Preconditions.checkArgument;
//
//
//@Getter
//@Setter
//public class Hagrid
//{
//    static protected AbstractMetric metric = new PrometheusMetric();
//    static HttpServer hagridMetricServer = null;
//
//    static HashMap<String, Long> offsetMapping = new HashMap<>();
//
//    static Logger log = LoggerFactory.getLogger(Hagrid.class.getName());
//
//    protected String discoveryId;
//
//    protected  JsonNode hagridConfiguration;
//
//    protected TreeNode<String> DAG = null;
//
//    protected ImmutableMultimap<String, String> serviceAssetTable;
//
//    protected AbstractInfra infra;
//
//
//    public Hagrid(String hagridConfigFileInResourceDir) throws Exception {
//        this.hagridConfiguration = Utility.loadHagridConfiguration(hagridConfigFileInResourceDir);
//        this.discoveryId = UUID.randomUUID().toString();
//        init();
//    }
//
//
//    private void init() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
//
//        Init init = new Init();
//
//        // Init the DAG
//        final TreeNode<String> dag = init.scanSteps();
//        checkArgument(dag.size() > 0, "Directed Acyclic Graph for the service can not be null");
//        this.DAG = dag;
//
//        // Init the AssetTable
//        final ImmutableMultimap<String, String> serviceAssetTable = init.scanAssets();
//        checkArgument(serviceAssetTable.size() > 0, "Service assets table for the service can not be null");
//        this.serviceAssetTable = serviceAssetTable;
//
//        // Init the Hagrid InfraFactory
//        this.infra = init.newInfra();
//
//        if(hagridMetricServer == null){
//            init.exposeMetricServer();
//        }
//
//        SyncStatusService.setKeyValue(Constants.SYNC_STATUS_KEY, Constants.SYNC_STATUS.IN_PROGRESS);
//    }
//
//    public String run() throws Exception {
//
//        final AbstractInfra infra = this.infra;
//        final TreeNode<String> DAG = this.DAG;
//        final ImmutableMultimap<String, String> serviceAssetTable = this.serviceAssetTable;
//        final AbstractMetric metric = this.metric;
//        final JsonNode hagridConfiguration = this.hagridConfiguration;
//
//        TraverserExecutorService.submit(new Callable<Void>() {
//            @Override
//            public Void call() throws Exception {
//                HagridExecutor.execute(infra, DAG, serviceAssetTable, metric, hagridConfiguration);
//                TearDown tearDown = new TearDown();
//                tearDown.stopMetricServer();
//                tearDown.shutdownThreadExecutor();
//                tearDown.shutdownInfra();
//                return null;
//            }
//        });
//
//        return this.discoveryId;
//    }
//
//    private class Init{
//
//        private TreeNode<String> scanSteps() throws ClassNotFoundException {
//
//            DagService scanSteps = new DagService();
//            final TreeNode<String> dag = scanSteps.scanner(hagridConfiguration);
//            log.info("\n DAG \n---------------\n {} \n--------------", dag);
//            log.info("Beans are scanned");
//            return dag;
//        }
//
//        private ImmutableMultimap<String, String> scanAssets() throws ClassNotFoundException {
//
//            AssetBeanDependencyService scanAssets = new AssetBeanDependencyService();
//            return ImmutableMultimap.copyOf(scanAssets.scanner(DAG, hagridConfiguration));
//        }
//
//        private AbstractInfra newInfra() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
//
//            return InfraFactory.getNewInfra(discoveryId, hagridConfiguration);
//        }
//
//        private void exposeMetricServer(){
//            try {
//
//                hagridMetricServer = HttpServer.create(new InetSocketAddress(7070), 0);
//                hagridMetricServer.createContext("/metrics", httpExchange -> {
//                    String response = metric.scrape();
//                    httpExchange.sendResponseHeaders(200, response.getBytes().length);
//                    try (OutputStream os = httpExchange.getResponseBody()) {
//                        os.write(response.getBytes());
//                    }
//                });
//                hagridMetricServer.start();
//            }
//            catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//    }
//
//    private class TearDown{
//
//        private void stopMetricServer(){
//            // Stop metric server after 30 seconds delay
//            hagridMetricServer.stop(30);
//        }
//
//        private void shutdownThreadExecutor(){
//            TraverserExecutorService.shutdownNow();
//        }
//
//        private void shutdownInfra(){
//            // Here delete all infra related stuff
//        }
//
//    }
//
//    public class ConsumerService {
//
//        String consumerId;
//        String consumerGroup;
//
//        AbstractQueue abstractQueue = infra.getAbstractQueue("publisher");
//
//        public ConsumerService(String consumerGroup){
//            this.consumerGroup = consumerGroup;
//            this.consumerId = UUID.randomUUID().toString();
//        }
//
//        public List<String> getAssets(int n) throws Exception {
//            return abstractQueue.poll(n);
//        }
//        private void getAssetsAsBlocking(int n){
//
//        }
//
//        public String getDiscoveryStatus(){
//            return (String)SyncStatusService.getValueByKey(Constants.SYNC_STATUS_KEY).toString();
//        }
//
//        private void getTraverserStatus(){
//
//        }
//
//        private void getProcessorStatus(){
//
//        }
//
//        private void getCurrentRunningStep(){
//
//        }
//
//        private void getBeansDiscoveredForStep(AbstractStep abstractStep){
//
//        }
//
//        private void getCurrentRunningStep(int n){
//
//        }
//
//    }
//
//
////    public static void main(String args[]) throws Exception {
////
////        Hagrid hagrid = new Hagrid("hagrid.yaml");
////        hagrid.run();
////        Hagrid.ConsumerService consumer = hagrid.new ConsumerService("group1");
////        while (Boolean.FALSE.equals(consumer.getDiscoveryStatus().equals("SUCCESS"))){
////            List<String> x = consumer.getAssets(100);
////            Iterator<String> it  = x.iterator();
////            while(it.hasNext()){
////                System.out.println(it.next());
////            }
////        }
////    }
//
//}
