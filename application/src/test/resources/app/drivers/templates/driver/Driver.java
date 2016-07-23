package cloud.benchflow.experiment.drivers;

import com.sun.faban.driver.*;
import com.sun.faban.driver.transport.hc3.ApacheHC3Transport;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.logging.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import cloud.benchflow.monitors.*;

public class Driver {

    private DriverContext ctx;
//    private HttpTransport http;
    private ApacheHC3Transport http;
    private String sutEndpoint;
    private Logger logger;
    private Map<String,String> modelsStartID;
    private Map<String, ServiceInfo> serviceInfoMap;

    //add plugin with spoon

    public Driver() throws Exception {
        initialize();
        setSutEndpoint();
        //added with spoon
        //loadModelsInfo();
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

    /**
     * Retrieves the address for a benchflow service from the deployment manager
     */
    private String benchFlowServiceAddress(String benchFlowServiceId, String privatePort) throws Exception {

        String deploymentManagerPortsApi = benchFlowComposeAddress + "/projects/" +
                trialId + "/port/" + benchFlowServiceId + "/" + privatePort;

        HttpMethod getServiceAddress = new GetMethod(deploymentManagerPortsApi);
        http.getHttpClient().executeMethod(getServiceAddress);
        String serviceAddress = new String(getServiceAddress.getResponseBody(), "UTF-8");
        getServiceAddress.releaseConnection();
        return serviceAddress;

    }


    @OnceBefore
    public void onceBefore() throws Exception {
        //We wait a bit to create a gap in the data (TODO-RM: experimenting with data cleaning)
        //and be sure the model started during the warm up and timing synch of the sistem, end,
        //event though now that we use mock models they end very fast
        Thread.sleep(20000);
        logger.info("Tested pre-run (sleep 20) done");

        String privatePort = driverConfig.getXPathValue("benchFlowServices/privatePort");
        for(ServiceInfo serviceInfo: serviceInfoMap.values()) {

            List<CollectorInfo> collectorInfoList = serviceInfo.getCollectors();
            for(CollectorInfo collectorInfo: collectorInfoList) {

                List<MonitorInfo> monitorInfoList = collectorInfo.getMonitors();
                for(MonitorInfo monitorInfo: monitorInfoList) {

                    //monitor at start of load
                    if(monitorInfo.getRunPhase().equals("start")) {

                        String monitorEndpoint = benchFlowServiceAddress(monitorInfo.getId(), privatePort);
                        Monitor monitor = MonitorFactory.getMonitor(
                                monitorInfo.getName(),
                                monitorInfo.getParams(),
                                monitorEndpoint,
                                monitorInfo.startAPI,
                                monitorInfo.stopAPI,
                                monitorInfo.monitorAPI,
                                logger
                        );
                        //run = start + monitor + stop
                        monitor.run();
                    }

                    else if(monitorInfo.getRunPhase().equals("all")) {

                        String monitorEndpoint = benchFlowServiceAddress(monitorInfo.getId(), privatePort);
                        Monitor monitor = MonitorFactory.getMonitor(
                                monitorInfo.getName(),
                                monitorInfo.getParams(),
                                monitorEndpoint,
                                monitorInfo.startAPI,
                                monitorInfo.stopAPI,
                                monitorInfo.monitorAPI,
                                logger
                        );
                        monitor.start();
                    }

                }

                if(collectorInfo.startAPI != null) {
                    String collectorEndpoint = benchFlowServiceAddress(collectorInfo.getId(), privatePort);
                    http.fetchURL(collectorEndpoint + collectorInfo.getStartAPI());
                }

            }

        }

    }


    @OnceAfter
    //TODO: modify this
    public void onceAfter() {

        String privatePort = driverConfig.getXPathValue("benchFlowServices/privatePort");
        for(ServiceInfo serviceInfo: serviceInfoMap.values()) {

            List<CollectorInfo> collectorInfoList = serviceInfo.getCollectors();
            for(CollectorInfo collectorInfo: collectorInfoList) {

                List<MonitorInfo> monitorInfoList = collectorInfo.getMonitors();
                for(MonitorInfo monitorInfo: monitorInfoList) {

                    //monitor at end of load
                    if(monitorInfo.getRunPhase().equals("end")) {
                        String monitorEndpoint = benchFlowServiceAddress(monitorInfo.getId(), privatePort);

                        String monitorEndpoint = benchFlowServiceAddress(monitorInfo.getId(), privatePort);
                        Monitor monitor = MonitorFactory.getMonitor(
                                monitorInfo.getName(),
                                monitorInfo.getParams(),
                                monitorEndpoint,
                                monitorInfo.startAPI,
                                monitorInfo.stopAPI,
                                monitorInfo.monitorAPI,
                                logger
                        );

                        monitor.run();

                    } else if(monitorInfo.getRunPhase().equals("all")) {
                        String monitorEndpoint = benchFlowServiceAddress(monitorInfo.getId(), privatePort);
                        Monitor monitor = MonitorFactory.getMonitor(
                                monitorInfo.getName(),
                                monitorInfo.getParams(),
                                monitorEndpoint,
                                monitorInfo.startAPI,
                                monitorInfo.stopAPI,
                                monitorInfo.monitorAPI,
                                logger
                        );

                        monitor.monitor();
                        monitor.stop();
                    }

                }

                String collectorEndpoint = benchFlowServiceAddress(collectorInfo.getId(), privatePort);
                http.fetchURL(collectorEndpoint + collectorInfo.getStopAPI());
            }

        }
    }


    private void setSutEndpoint() throws Exception {
        sutEndpoint = getXPathValue("sutConfiguration/sutEndpoint");
    }

    private boolean isStarted() {

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

    //TODO: add with spoon?
    private void loadModelsInfo() {
        int numModel = Integer.parseInt(getContextProperty("model_num"));
        for (int i = 1; i <= numModel; i++) {
            String name = getContextProperty("model_" + i + "_name");
            String startID = getContextProperty("model_" + i + "_startID");
            modelsStartID.put(name, startID);
        }
    }

    private void initialize() {
        ctx = DriverContext.getContext();
        HttpTransport.setProvider("com.sun.faban.driver.transport.hc3.ApacheHC3Transport");
        http = (ApacheHC3Transport) HttpTransport.newInstance();
        logger = ctx.getLogger();
        modelsStartID = new HashMap<String,String>();
        serviceInfoMap = new HashMap<String, ServiceInfo>();
        parseBenchmarkConfiguration();
    }

    private String getContextProperty(String property){
        return ctx.getProperty(property);
    }

    private String getXPathValue(String xPathExpression) throws Exception {
        return ctx.getXPathValue(xPathExpression);
    }

    /**
     * Parse run.xml to build a service info map
     */
    private void parseBenchmarkConfiguration() {

        NodeList services = driverConfig.getNode("benchFlowServices/servicesConfiguration").getChildNodes();

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

                    Element currentAPI = (Element)collectorAPIs.item(apiIndex);
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