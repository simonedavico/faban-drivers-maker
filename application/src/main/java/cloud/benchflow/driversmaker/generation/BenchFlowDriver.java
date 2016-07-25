package cloud.benchflow.driversmaker.generation;

import cloud.benchflow.driversmaker.generation.utils.BenchmarkUtils;
import cloud.benchflow.monitors.Monitor;
import cloud.benchflow.monitors.MonitorFactory;

import com.sun.faban.driver.*;
import com.sun.faban.driver.transport.hc3.ApacheHC3Transport;

import org.apache.commons.lang3.StringEscapeUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 25/07/16.
 *
 * Base BenchFlow driver. Handles monitors and collectors lifecycle
 */
public class BenchFlowDriver {

    protected DriverContext ctx;
    protected ApacheHC3Transport http;
    protected String sutEndpoint;
    protected String trialId;

    private Logger logger;
    private Map<String, ServiceInfo> serviceInfoMap;
    private String deploymentManagerAddress;

    public BenchFlowDriver() throws Exception {
        initialize();
        setSutEndpoint();
    }

    protected String getContextProperty(String property){
        return ctx.getProperty(property);
    }

    protected String getXPathValue(String xPathExpression) throws Exception {
        return ctx.getXPathValue(xPathExpression);
    }

    private static class ServiceInfo {

        private String serviceName;
        private List<CollectorInfo> collectors;

        public ServiceInfo(String name) {
            serviceName = name;
        }

        public void addCollector(CollectorInfo collector) {
            collectors.add(collector);
        }

        public String getName() {
            return serviceName;
        }

        public List<CollectorInfo> getCollectors() {
            return collectors;
        }

    }

    private static class CollectorInfo {

        private String collectorName;
        private String collectorId;
        private List<MonitorInfo> monitors;
        private String startAPI;
        private String stopAPI;

        public CollectorInfo(String name, String id) {
            collectorName = name;
            collectorId = id;
        }

        public String getName() {
            return collectorName;
        }

        public String getId() {
            return collectorId;
        }

        public List<MonitorInfo> getMonitors() {
            return monitors;
        }

        public String getStartAPI() {
            return startAPI;
        }

        public String getStopAPI() {
            return stopAPI;
        }

        public void setStartAPI(String start) {
            startAPI = start;
        }

        public void setStopAPI(String stop) {
            stopAPI = stop;
        }

        public void addMonitor(MonitorInfo monitor) {
            monitors.add(monitor);
        }

    }

    private static class MonitorInfo {

        private String monitorName;
        private String monitorId;

        private String startAPI;
        private String monitorAPI;
        private String stopAPI;

        private String runPhase;
        private Map<String, String> params;

        public MonitorInfo(String name, String id) {
            monitorName = name;
            monitorId = id;
        }

        public String getName() {
            return monitorName;
        }

        public String getId() {
            return monitorId;
        }

        public String getStartAPI() {
            return startAPI;
        }

        public String getStopAPI() {
            return stopAPI;
        }

        public String getMonitorAPI() {
            return monitorAPI;
        }

        public void setStartAPI(String start) {
            startAPI = start;
        }

        public void setStopAPI(String stop) {
            stopAPI = stop;
        }

        public void setMonitorAPI(String monitor) {
            monitorAPI = monitor;
        }

        public String getRunPhase() {
            return runPhase;
        }

        public void setRunPhase(String phase) {
            runPhase = phase;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public void setParams(Map<String, String> p) {
            params = p;
        }

    }

    private void iterateOverBenchFlowServices(
            Consumer<ServiceInfo> serviceInfoConsumer,
            Consumer<CollectorInfo> collectorInfoConsumer,
            Consumer<MonitorInfo> monitorInfoConsumer) {

        for (ServiceInfo serviceInfo: serviceInfoMap.values()) {

            List<CollectorInfo> collectorInfoList = serviceInfo.getCollectors();
            for (CollectorInfo collectorInfo : collectorInfoList) {

                List<MonitorInfo> monitorInfoList = collectorInfo.getMonitors();
                for (MonitorInfo monitorInfo : monitorInfoList) {
                    monitorInfoConsumer.accept(monitorInfo);
                }

                collectorInfoConsumer.accept(collectorInfo);
            }

            serviceInfoConsumer.accept(serviceInfo);
        }

    }

    @OnceBefore
    public void onceBefore() throws Exception {
        //We wait a bit to create a gap in the data (TODO-RM: experimenting with data cleaning)
        //and be sure the model started during the warm up and timing synch of the sistem, end,
        //event though now that we use mock models they end very fast
        Thread.sleep(20000);
        logger.info("Tested pre-run (sleep 20) done");

        final String privatePort = getXPathValue("benchFlowServices/privatePort");

        //do nothing for services
        Consumer<ServiceInfo> serviceInfoConsumer = new Consumer<ServiceInfo>() {
            @Override
            public void accept(ServiceInfo serviceInfo) {
                //do nothing
            }
        };

        //if runPhase == start -> start, monitor, stop
        //if runPhase == all -> start
        Consumer<MonitorInfo> monitorInfoConsumer = new Consumer<MonitorInfo>() {
            @Override
            public void accept(MonitorInfo monitorInfo) {

                Monitor monitor = null;
                String monitorEndpoint = null;

                if(monitorInfo.getRunPhase().equals("start")) {

                    try {
                        monitorEndpoint = BenchmarkUtils.benchFlowServiceAddress(
                                deploymentManagerAddress,
                                privatePort,
                                monitorInfo.getId(),
                                trialId,
                                http);

                        monitor = MonitorFactory.getMonitor(
                                monitorInfo.getName(),
                                monitorInfo.getParams(),
                                monitorEndpoint,
                                monitorInfo.startAPI,
                                monitorInfo.stopAPI,
                                monitorInfo.monitorAPI,
                                logger);

                        monitor.run();

                    } catch (Exception e) {
                        //decide what to do here
                        e.printStackTrace();
                    }

                } else if(monitorInfo.getRunPhase().equals("all")) {

                    try {
                        monitorEndpoint = BenchmarkUtils.benchFlowServiceAddress(
                                deploymentManagerAddress,
                                privatePort,
                                monitorInfo.getId(),
                                trialId,
                                http);

                        monitor = MonitorFactory.getMonitor(
                                monitorInfo.getName(),
                                monitorInfo.getParams(),
                                monitorEndpoint,
                                monitorInfo.startAPI,
                                monitorInfo.stopAPI,
                                monitorInfo.monitorAPI,
                                logger);

                        monitor.start();
                    } catch (Exception e) {
                        //decide what  to do here
                        e.printStackTrace();
                    }

                }

            }

        };

        //if startApi -> start
        Consumer<CollectorInfo> collectorInfoConsumer = new Consumer<CollectorInfo>() {
            @Override
            public void accept(CollectorInfo collectorInfo) {

                if(collectorInfo.startAPI != null) {

                    try {
                        String collectorEndpoint = BenchmarkUtils.benchFlowServiceAddress(
                                deploymentManagerAddress,
                                privatePort,
                                collectorInfo.getId(),
                                trialId,
                                http);

                        http.fetchURL(collectorEndpoint + collectorInfo.getStartAPI());

                    } catch (Exception e) {
                        //decide what to do here
                        e.printStackTrace();
                    }

                }

            }
        };

        iterateOverBenchFlowServices(
                serviceInfoConsumer,
                collectorInfoConsumer,
                monitorInfoConsumer
        );

    }


    //if monitor == end -> start, monitor, stop
    //if monitor == all -> monitor, stop
    //collector -> stop
    @OnceAfter
    public void onceAfter() throws Exception {

        final String privatePort = getXPathValue("benchFlowServices/privatePort");

        Consumer<ServiceInfo> serviceInfoConsumer = new Consumer<ServiceInfo>() {
            @Override
            public void accept(ServiceInfo serviceInfo) {
                //do nothing
            }
        };

        Consumer<MonitorInfo> monitorInfoConsumer = new Consumer<MonitorInfo>() {
            @Override
            public void accept(MonitorInfo monitorInfo) {

                String monitorEndpoint;
                Monitor monitor;

                if(monitorInfo.getRunPhase().equals("end")) {

                    try {
                        monitorEndpoint = BenchmarkUtils.benchFlowServiceAddress(
                                deploymentManagerAddress,
                                privatePort,
                                monitorInfo.getId(),
                                trialId,
                                http);

                        monitor = MonitorFactory.getMonitor(
                                monitorInfo.getName(),
                                monitorInfo.getParams(),
                                monitorEndpoint,
                                monitorInfo.startAPI,
                                monitorInfo.stopAPI,
                                monitorInfo.monitorAPI,
                                logger);

                        monitor.run();
                    } catch (Exception e) {
                        //decide what to do here
                        e.printStackTrace();
                    }


                } else if(monitorInfo.getRunPhase().equals("all")) {

                    try {
                        monitorEndpoint = BenchmarkUtils.benchFlowServiceAddress(
                                deploymentManagerAddress,
                                privatePort,
                                monitorInfo.getId(),
                                trialId,
                                http);

                        monitor = MonitorFactory.getMonitor(
                                monitorInfo.getName(),
                                monitorInfo.getParams(),
                                monitorEndpoint,
                                monitorInfo.startAPI,
                                monitorInfo.stopAPI,
                                monitorInfo.monitorAPI,
                                logger);

                        monitor.monitor();
                        monitor.stop();
                    } catch(Exception e) {
                        //decide what to do here
                        e.printStackTrace();
                    }

                }

            }
        };

        Consumer<CollectorInfo> collectorInfoConsumer = new Consumer<CollectorInfo>() {
            @Override
            public void accept(CollectorInfo collectorInfo) {

                try {

                    String collectorEndpoint = BenchmarkUtils.benchFlowServiceAddress(
                            deploymentManagerAddress,
                            privatePort,
                            collectorInfo.getId(),
                            trialId,
                            http);

                    http.fetchURL(collectorEndpoint + collectorInfo.getStopAPI());

                } catch(Exception e) {
                    //decide what to do here
                    e.printStackTrace();
                }

            }
        };

        iterateOverBenchFlowServices(
                serviceInfoConsumer,
                collectorInfoConsumer,
                monitorInfoConsumer
        );
    }


    private void setSutEndpoint() throws Exception {
        sutEndpoint = getXPathValue("sutConfiguration/sutEndpoint");
    }

    protected boolean isStarted() {

        long steadyStateStartTime = ctx.getSteadyStateStartNanos();
        //If we don't have the steadyStateStartTime, it means it is not yet set,
        //then we are not during the run
        if( steadyStateStartTime!=0 ){

            long rampUpTime = ctx.getRampUp() * 1000000000l;
            long steadyStateTime = ctx.getSteadyState() * 1000000000l;
            long rampDownTime = ctx.getRampDown() * 1000000000l;

            long rampUpStartTime = steadyStateStartTime - rampUpTime;
            long steadyStateEndTime = steadyStateStartTime + steadyStateTime;
            long rampDownEndTime = steadyStateEndTime + rampDownTime;

            long currentTime = ctx.getNanoTime();

            logger.info("rampUpTime: " + rampUpTime);
            logger.info("steadyStateTime: " + steadyStateTime);
            logger.info("rampDownTime: " + rampDownTime);
            logger.info("rampUpStartTime: " + rampUpStartTime);
            logger.info("steadyStateEndTime: " + steadyStateEndTime);
            logger.info("rampDownEndTime: " + rampDownEndTime);
            logger.info("steadyStateStartTime: " + steadyStateStartTime);
            logger.info("currentTime: " + currentTime);

            return (rampUpStartTime <= currentTime) && (currentTime <= rampDownEndTime);
        }

        return false;
    }

    protected void initialize() throws Exception {
        ctx = DriverContext.getContext();
        HttpTransport.setProvider("com.sun.faban.driver.transport.hc3.ApacheHC3Transport");
        http = (ApacheHC3Transport) HttpTransport.newInstance();
        logger = ctx.getLogger();
        serviceInfoMap = new HashMap<String, ServiceInfo>();
        parseBenchmarkConfiguration();
    }

    /**
     * Parse run.xml to build a service info map
     */
    private void parseBenchmarkConfiguration() throws Exception {

        logger.info("About to parse serialized BenchFlow services configuration");

        String benchFlowServicesSerializedNode = getContextProperty("benchFlowServices");
        benchFlowServicesSerializedNode = StringEscapeUtils.unescapeXml(benchFlowServicesSerializedNode);
        Node benchFlowServices = BenchmarkUtils.stringToNode(benchFlowServicesSerializedNode);

        logger.info("Successfully parsed BenchFlow services configuration");

        deploymentManagerAddress = getXPathValue("benchFlowServices/deploymentManager");
        trialId = getXPathValue("benchFlowRunConfiguration/trialId");

        NodeList services = benchFlowServices.getChildNodes();

        //iterate over every service in the configuration
        for(int serviceIndex = 0; serviceIndex < services.getLength(); serviceIndex++) {

            Element service = (Element) services.item(serviceIndex);
            String serviceName = service.getAttribute("name");
            ServiceInfo serviceInfo = new ServiceInfo(serviceName);

            NodeList collectors = service.getElementsByTagName("collector");

            //iterate over all collectors for the given service
            for(int collectorIndex = 0; collectorIndex < collectors.getLength(); collectorIndex++) {

                Element collector = (Element) collectors.item(collectorIndex);
                String collectorName = collector.getAttribute("name");
                String collectorId = collector.getElementsByTagName("id").item(0).getTextContent();

                CollectorInfo collectorInfo = new CollectorInfo(collectorName, collectorId);

                Element apiNode = (Element) collector.getElementsByTagName("api").item(0);

                //find out start and stop API values
                NodeList collectorAPIs = apiNode.getChildNodes();
                for(int apiIndex = 0; apiIndex < collectorAPIs.getLength(); apiIndex++) {

                    Element currentAPI = (Element) collectorAPIs.item(apiIndex);

                    if(currentAPI.getTagName().equals("start")) {
                        collectorInfo.setStartAPI(currentAPI.getTextContent());
                    } else if(currentAPI.getTagName().equals("stop")) {
                        collectorInfo.setStopAPI(currentAPI.getTextContent());
                    }

                }

                NodeList monitors = collector.getElementsByTagName("monitors").item(0).getChildNodes();

                for(int monitorIndex = 0; monitorIndex < monitors.getLength(); monitorIndex++) {

                    Element monitor = (Element) monitors.item(monitorIndex);
                    String monitorName = monitor.getAttribute("name");
                    String monitorId = monitor.getElementsByTagName("id").item(0).getTextContent();

                    MonitorInfo monitorInfo = new MonitorInfo(monitorName, monitorId);

                    //build monitor parameters map
                    Element monitorConfiguration = (Element) monitor.getElementsByTagName("configuration").item(0);
                    NodeList monitorConfigurationParams = monitorConfiguration.getElementsByTagName("param");
                    Map<String, String> params = new HashMap<String, String>();
                    for(int paramIndex = 0; paramIndex < monitorConfigurationParams.getLength(); paramIndex++) {

                        Element param = (Element) monitorConfigurationParams.item(paramIndex);
                        String paramName = param.getAttribute("name");
                        String paramValue = param.getTextContent();
                        params.put(paramName, paramValue);

                    }

                    monitorInfo.setParams(params);

                    Element monitorApiNode = (Element) monitor.getElementsByTagName("api").item(0);

                    //find out monitor APIs
                    NodeList monitorAPIs = monitorApiNode.getChildNodes();
                    for(int monitorApiIndex = 0; monitorApiIndex < monitorAPIs.getLength(); monitorApiIndex++) {

                        Element currentMonitorAPI = (Element) monitorAPIs.item(monitorApiIndex);
                        if(currentMonitorAPI.getTagName().equals("start")) {
                            monitorInfo.setStartAPI(currentMonitorAPI.getTextContent());
                        } else if(currentMonitorAPI.getTagName().equals("monitor")) {
                            monitorInfo.setMonitorAPI(currentMonitorAPI.getTextContent());
                        } else if(currentMonitorAPI.getTagName().equals("stop")) {
                            monitorInfo.setStopAPI(currentMonitorAPI.getTextContent());
                        }

                    }

                    //check phase
                    String runPhase = monitor.getElementsByTagName("runPhase").item(0).getTextContent();
                    monitorInfo.setRunPhase(runPhase);

                    collectorInfo.addMonitor(monitorInfo);
                }

                serviceInfo.addCollector(collectorInfo);
            }

            serviceInfoMap.put(serviceName, serviceInfo);
        }

    }

}
