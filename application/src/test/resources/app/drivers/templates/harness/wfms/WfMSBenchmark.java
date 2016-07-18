package cloud.benchflow.experiment.harness;

import com.sun.faban.harness.*;
import com.sun.faban.driver.transport.hc3.ApacheHC3Transport;

import java.util.*;
import java.util.logging.Logger;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WfMSBenchmark extends DefaultFabanBenchmark2 {

    private static Logger logger = Logger.getLogger(cloud.benchflow.experiment.harness.WfMSBenchmark.class.getName());

    private String benchFlowComposeAddress;
    private String sutEndpoint;
    private String trialId;
    protected Path benchmarkDir;

    private ApacheHC3Transport http;
    private DriverConfig driverConfig;
    private Map<String, String> modelsStartID;
    private Map<String, ServiceInfo> serviceInfoMap;
    //this is already in defaultfabanbenchmark2
//    protected ParamRepository params;

    /**
     * Encapsulates methods to retrieve values from run.xml
     */
    private class DriverConfig {

        private Node getNode(String xPathExpression) {
            return params.getNode(xPathExpression);
        }

        private NodeList getNodes(String xPathExpression) {
            return params.getNodes(xPathExpression);
        }

        private String getXPathValue(String xPathExpression) throws RuntimeException {
            try {
                return params.getParameter(xPathExpression);
            } catch(Exception e) {
               throw new RuntimeException(e.getMessage(), e);
            }
        }

        private Element addConfigurationNode(Element parent, String nodeName, String value) throws Exception {
            Element node = params.addParameter(parent, null, null, nodeName);
            params.setParameter(node, value);
            params.save();
            return node;
        }

        //TODO: avoid multiple params.save(); calls and setup a dedicated method to be called at the end of each Faban Driver operations
        private Element addConfigurationNode(String baseXPath, String nodeName, String value) throws Exception {
            Element node = params.addParameter(baseXPath, null, null, nodeName);
            params.setParameter(node, value);
            params.save();
            return node;
        }

        private void addProperty(Element properties, String name, String value) throws Exception {
            // Document runDoc = params.getNode("benchFlowBenchmark").getOwnerDocument();
            Document runDoc = params.getTopLevelElements().item(0).getOwnerDocument();
            Element prop = addConfigurationNode(properties,"property","");
            prop.setAttribute("name",name);
            prop.appendChild(runDoc.createTextNode(value));
            properties.appendChild(prop);
            params.save();
        }

        private void addModel(Element properties, int modelNum, String modelName, String processDefinitionId) throws Exception {
            //We need to attach them as driver properties otherwise it is not possible to access them in the Driver
            //Add the information about the deployed process in the run context
            //TODO: provide abstracted method to improve the adding of informations like the following, dinamically
            //Maybe also improving com.sun.faban.harness.ParamRepository if needed
            /**
             * <models>
             *  <model id="processDefinitionId">
             *   <name></name>
             *   <startID></startID>
             *  </model>
             * </models>
             */
            addProperty(properties, "model_" + modelNum + "_name", modelName);
            addProperty(properties, "model_" + modelNum + "_startID", processDefinitionId);
        }

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


    /***
     * This method deploys the sut.
     */
    @Configure
    public void configure() throws Exception {

        Path sutDir = benchmarkDir.resolve("sut");
        File dockerCompose = sutDir.resolve("/docker-compose-" + trialId + ".yml").toFile();
        FilePart dockerComposeFile = new FilePart("docker_compose_file", dockerCompose);
        String deployAPI = benchFlowComposeAddress + "/projects/" + trialId + "/deploymentDescriptor/";
        PutMethod put = new PutMethod(deployAPI);

        Part[] partsArray = { dockerComposeFile };
        put.setRequestEntity(new MultipartRequestEntity(partsArray, put.getParams()));
        int status = http.getHttpClient().executeMethod(put);

        logger.info("System Deployed. Status: " + status);

        String upAPI = benchFlowComposeAddress + "/projects/" + trialId + "/up/";
        PutMethod putUp = new PutMethod(upAPI);
        int statusUp = http.getHttpClient().executeMethod(putUp);

        logger.info("System Started. Status: " + statusUp);
    }

    /**
     * This method sets sut endpoint, deployment manager address, trial id
     */
    @Override
    @Validate
    public void validate() throws Exception {
        super.validate();

        logger.info("START: Validate...");

        initialize();

        logger.info("DONE: initialize");
        logger.info("benchmarkDir is: " + benchmarkDir.toString());

        setSutEndpoint();
        benchFlowComposeAddress = driverConfig.getXPathValue("benchFlowServices/deploymentManager");
        trialId = driverConfig.getXPathValue("benchFlowRunConfiguration/trialId");

        logger.info("Deployment manager address is: " + benchFlowComposeAddress);
        logger.info("Trial id is: " + trialId);
        logger.info("DONE: setSutEndpoint");

        driverConfig.addConfigurationNode("sutConfiguration","sutEndpoint",getSutEndpoint());

        logger.info("DONE: addConfigurationNode");
        logger.info("END: Validate...");

    }

    /**
     *  Deploys BPMN models
     */
    @PreRun
    public void preRun() throws Exception {

        logger.info("START: Deploying processes...");

        int numDeplProcesses = 0;
        Path modelDir = benchmarkDir.resolve("models");
        String deployAPI = getSutEndpoint() + "/deployment/create";
        String processDefinitionAPI = getSutEndpoint() + "/process-definition";
        File folder = modelDir.toFile();
        File[] listOfFiles = folder.listFiles();

        //Add models node
        String agentName = "WfMSBenchmarkDriver";
        String driverToUpdate = "fa:runConfig/fd:driverConfig[@name=\"" + agentName + "\"]";

        //Here I am assuming there is not an already defined properties element
        Element properties = driverConfig.addConfigurationNode(driverToUpdate,"properties","");

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String modelName = listOfFiles[i].getName();
                String modelPath = modelDir + "/" + modelName;
                File modelFile = new File(modelPath);
                String processDefinitionId = null;

                //TODO: add with spoon
//              processDefinitionId = plugin.deploy(modelFile).get(modelName);

                logger.info("PROCESS DEFINITION ID: " + processDefinitionId);

                driverConfig.addModel(properties, i+1, modelName,processDefinitionId);
                numDeplProcesses++;
            }
        }

        driverConfig.addProperty(properties, "model_num", String.valueOf(numDeplProcesses));
        logger.info("END: Deploying processes...");
    }

    /**
     * Undeploys the sut
     */
    private int undeploy() throws Exception {
        //remove the sut
        //curl -v -X PUT http://<HOST_IP>:<HOST_PORT>/projects/camunda/rm/
        String rmAPI = benchFlowComposeAddress + "/projects/" + trialId + "/rm/";
        PutMethod putRm = new PutMethod(rmAPI);
        int statusRm = http.getHttpClient().executeMethod(putRm);
        return statusRm;
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


    /**
     * Starts collectors and related monitors
     */
    @Override
    @StartRun
    public void start() throws Exception {
        super.start();
        //callCollectorsApi("start");
        String privatePort = driverConfig.getXPathValue("benchFlowServices/privatePort");
        for(ServiceInfo serviceInfo: serviceInfoMap.values()) {

            List<CollectorInfo> collectorInfoList = serviceInfo.getCollectors();
            for(CollectorInfo collectorInfo: collectorInfoList) {

                List<MonitorInfo> monitorInfoList = collectorInfo.getMonitors();
                for(MonitorInfo monitorInfo: monitorInfoList) {

                    //monitor at start of load
                    if(monitorInfo.getRunPhase().equals("start")) {

                        String monitorEndpoint = benchFlowServiceAddress(monitorInfo.getId(), privatePort);
                        //TODO: get monitor interface from factory and run .run() method
                    }

                    else if(monitorInfo.getRunPhase().equals("all")) {
                        String monitorEndpoint = benchFlowServiceAddress(monitorInfo.getId(), privatePort);
                        //TODO: get monitor interface from factory and run .start() method
                    }

                }

                String collectorEndpoint = benchFlowServiceAddress(collectorInfo.getId(), privatePort);
                //TODO: call start api for collector
            }

        }
    }

    /**
     * Stops collectors and related monitors
     */
    @PostRun
    public void postRun() throws Exception {

        String privatePort = driverConfig.getXPathValue("benchFlowServices/privatePort");
        for(ServiceInfo serviceInfo: serviceInfoMap.values()) {

            List<CollectorInfo> collectorInfoList = serviceInfo.getCollectors();
            for(CollectorInfo collectorInfo: collectorInfoList) {

                List<MonitorInfo> monitorInfoList = collectorInfo.getMonitors();
                for(MonitorInfo monitorInfo: monitorInfoList) {

                    //monitor at end of load
                    if(monitorInfo.getRunPhase().equals("end")) {
                        String monitorEndpoint = benchFlowServiceAddress(monitorInfo.getId(), privatePort);
                        //TODO: get monitor interface from factory and run .run() method
                    } else if(monitorInfo.getRunPhase().equals("all")) {
                        String monitorEndpoint = benchFlowServiceAddress(monitorInfo.getId(), privatePort);
                        //TODO: get monitor interface from factory and run .monitor() and .stop() methods
                    }

                }

                String collectorEndpoint = benchFlowServiceAddress(collectorInfo.getId(), privatePort);
                //TODO: call stop api for collector
            }

        }

        undeploy();
    }


    @Override
    @EndRun
    public void end() throws Exception {
        try {
            super.end();
        } catch (Exception e) {
            undeploy();
        }
    }

    @KillRun
    public void kill() throws Exception {
        undeploy();
    }

    protected final String getSutEndpoint() { return this.sutEndpoint; }

    protected final String getTrialId() { return this.trialId; }

    /**
     * Retrieves sut endpoint address from deployment manager and sets resolved address as field of this class
     */
    private void setSutEndpoint() throws Exception {

        StringBuilder urlBuilder = new StringBuilder();
        String targetServiceName = driverConfig.getXPathValue("sutConfiguration/serviceName");
        String targetServiceEndpoint = driverConfig.getXPathValue("sutConfiguration/endpoint");
        String privatePort = driverConfig.getXPathValue("benchFlowServices/privatePort");

        String targetServiceAddress = benchFlowServiceAddress(targetServiceName, privatePort);

        sutEndpoint = urlBuilder.append("http://")
                .append(targetServiceAddress)
                .append(targetServiceEndpoint).toString();

        driverConfig.addConfigurationNode("sutConfiguration", "sutEndpoint", sutEndpoint);
    }

    /**
     * Setup benchmarkDir, http transport, and services info map
     */
    private void initialize() {
        //params = RunContext.getParamRepository();
        this.benchmarkDir = Paths.get(RunContext.getBenchmarkDir());
        this.http = new ApacheHC3Transport();
        this.driverConfig = new DriverConfig();
        this.serviceInfoMap = new HashMap<String, ServiceInfo>();
        parseBenchmarkConfiguration();
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