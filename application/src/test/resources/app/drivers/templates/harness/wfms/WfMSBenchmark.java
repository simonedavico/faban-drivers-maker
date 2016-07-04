package cloud.benchflow.benchmark.harness;

import com.sun.faban.harness.*;
import com.sun.faban.driver.transport.hc3.ApacheHC3Transport;

import java.util.*;
import java.util.logging.Logger;
import java.util.concurrent.*;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WfMSBenchmark extends DefaultFabanBenchmark2 {

    private static Logger logger = Logger.getLogger(cloud.benchflow.benchmark.harness.WfMSBenchmark.class.getName());

    private String benchFlowComposeAddress;
    private String sutEndpoint;
    private String trialId;
    protected Path benchmarkDir;

    private ApacheHC3Transport http;
    private DriverConfig driverConfig;
    private Map<String, String> modelsStartID;
    //protected ParamRepository params;


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

    private class BenchFlowServicesAsynchInteraction implements Callable<String> {
        private String url;

        public BenchFlowServicesAsynchInteraction(String url){
            this.url = url;
        }

        @Override
        public String call() throws Exception {
            return http.fetchURL(url).toString();
        }
    }


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


    @Override
    @Validate
    public void validate() throws Exception {
        super.validate();

        logger.info("START: Validate...");

        initialize();

        logger.info("DONE: initialize");
        logger.info("benchmarkDir is: " + benchmarkDir.toString());

        setSutEndpoint();
        benchFlowComposeAddress = driverConfig.getXPathValue("benchFlowServices/benchFlowCompose");
        trialId = driverConfig.getXPathValue("benchFlowRunConfiguration/trialId");

        logger.info("Compose address is: " + benchFlowComposeAddress);
        logger.info("Trial id is: " + trialId);
        logger.info("DONE: setSutEndpoint");

        driverConfig.addConfigurationNode("sutConfiguration","sutEndpoint",getSutEndpoint());

        logger.info("DONE: addConfigurationNode");
        logger.info("END: Validate...");

    }

    @PreRun
    public void preRun() throws Exception {

        logger.info("START: Deployng processes...");

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

    private void callCollectorsApi(String api) throws Exception {

        //TODO: this is suboptimal because we create one thread for each collector
        //but we should really create one thread only for the collectors with a start API
        //this will allow us also to count the number of expected responses and use a countdownlatch
        NodeList collectors = driverConfig.getNode("benchFlowServices/collectors").getChildNodes();
        ExecutorService es = Executors.newFixedThreadPool(collectors.getLength());
        CompletionService<String> cs = new ExecutorCompletionService<>(es);

        List<Future<String>> collectorsStartResponses = new LinkedList<Future<String>>();
        for(int i = 0; i < collectors.getLength(); i++) {
            Node collector = collectors.item(i);
            String collectorName = collector.getNodeName();

            String[] bindings = driverConfig.getXPathValue("benchFlowServices/collectors/" + collectorName + "/bindings").split(",");
            for(String service : bindings) {

                //String completeCollectorName = collectorName + "_collector_" + service;
                String completeCollectorName = "benchflow.collector." + collectorName + "." + service;
                String collectorApi = driverConfig.getXPathValue("benchFlowServices/collectors/" + completeCollectorName + "/" + api);
                String privatePort = driverConfig.getXPathValue("benchFlowServices/collectors/" + completeCollectorName + "/privatePort");
                String portApi = benchFlowComposeAddress + "/projects/" + trialId + "/port/" + completeCollectorName + "/" + privatePort;

                HttpMethod getCollectorAddress = new GetMethod(portApi);
                http.getHttpClient().executeMethod(getCollectorAddress);
                String collectorAddress = new String(getCollectorAddress.getResponseBody(), "UTF-8");
                getCollectorAddress.releaseConnection();
                String collectorApiAddress = collectorAddress + collectorApi;
                collectorsStartResponses.add(cs.submit(new BenchFlowServicesAsynchInteraction(collectorApiAddress)));
            }
        }

    }

    private int undeploy() throws Exception {
        //remove the sut
        //curl -v -X PUT http://<HOST_IP>:<HOST_PORT>/projects/camunda/rm/
        String rmAPI = benchFlowComposeAddress + "/projects/" + trialId + "/rm/";
        PutMethod putRm = new PutMethod(rmAPI);
        int statusRm = http.getHttpClient().executeMethod(putRm);
        return statusRm;
    }


    @Override
    @StartRun
    public void start() throws Exception {
        super.start();
        callCollectorsApi("start");
    }

    @PostRun
    public void postRun() throws Exception {
        callCollectorsApi("stop");
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

    private void setSutEndpoint() throws Exception {

        StringBuilder urlBuilder = new StringBuilder();
        String targetServiceName = driverConfig.getXPathValue("sutConfiguration/serviceName");
        String targetServiceEndpoint = driverConfig.getXPathValue("sutConfiguration/endpoint");
        String targetServicePrivatePort = driverConfig.getXPathValue("sutConfiguration/privatePort");

        String portApi = benchFlowComposeAddress + "/projects/" + trialId + "/port/" + targetServiceName + "/" + targetServicePrivatePort;
        HttpMethod getTargetServiceAddress = new GetMethod(portApi);
        http.getHttpClient().executeMethod(getTargetServiceAddress);
        String targetServiceAddress = new String(getTargetServiceAddress.getResponseBody(), "UTF-8");

        getTargetServiceAddress.releaseConnection();

        sutEndpoint = urlBuilder.append("http://")
                .append(targetServiceAddress)
                .append(targetServiceEndpoint).toString();

        driverConfig.addConfigurationNode("sutConfiguration", "sutEndpoint", sutEndpoint);
    }

    private void initialize() {
//        params = RunContext.getParamRepository();
        this.benchmarkDir = Paths.get(RunContext.getBenchmarkDir());
        this.http = new ApacheHC3Transport();
        this.driverConfig = new DriverConfig();
    }

}