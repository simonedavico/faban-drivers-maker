package cloud.benchflow.driversmaker.generation;

import cloud.benchflow.driversmaker.generation.utils.RunXml;
import com.sun.faban.harness.*;
import com.sun.faban.driver.transport.hc3.ApacheHC3Transport;

import java.util.logging.Logger;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cloud.benchflow.driversmaker.generation.utils.BenchmarkUtils;

public class BenchFlowBenchmark extends DefaultFabanBenchmark2 {

    private static Logger logger = Logger.getLogger(BenchFlowBenchmark.class.getName());

    public String deploymentManagerAddress;
    public String sutEndpoint;
    public String trialId;
    public Path benchmarkDir;

    protected RunXml runXml;
    public ApacheHC3Transport http;

    /***
     * This method deploys the sut
     */
    @Configure
    public void configure() throws Exception {

        Path sutDir = benchmarkDir.resolve("sut");
        File dockerCompose = sutDir.resolve("/docker-compose-" + trialId + ".yml").toFile();
        FilePart dockerComposeFile = new FilePart("docker_compose_file", dockerCompose);
        String deployAPI = deploymentManagerAddress + "/projects/" + trialId + "/deploymentDescriptor/";
        PutMethod put = new PutMethod(deployAPI);

        Part[] partsArray = { dockerComposeFile };
        put.setRequestEntity(new MultipartRequestEntity(partsArray, put.getParams()));
        int status = http.getHttpClient().executeMethod(put);

        logger.info("System Deployed. Status: " + status);

        String upAPI = deploymentManagerAddress + "/projects/" + trialId + "/up/";
        PutMethod putUp = new PutMethod(upAPI);
        int statusUp = http.getHttpClient().executeMethod(putUp);

        logger.info("System Started. Status:" + statusUp);

        //loop on deployment manager logs
    }

    //public abstract boolean parseLog() <- returns true if log says "complete"




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
        deploymentManagerAddress = runXml.getXPathValue("benchFlowServices/deploymentManager");
        trialId = runXml.getXPathValue("benchFlowRunConfiguration/trialId");

        logger.info("Deployment manager address is: " + deploymentManagerAddress);
        logger.info("Trial id is: " + trialId);
        logger.info("DONE: setSutEndpoint");
        logger.info("END: Validate...");

    }

    /**
     * Undeploys the sut
     */
    protected int undeploy() throws Exception {
        //remove the sut
        //curl -v -X PUT http://<HOST_IP>:<HOST_PORT>/projects/camunda/rm/
        String rmAPI = deploymentManagerAddress + "/projects/" + trialId + "/rm/";
        PutMethod putRm = new PutMethod(rmAPI);
        int statusRm = http.getHttpClient().executeMethod(putRm);
        return statusRm;
    }

    /**
     * Undeploys the sut
     */
    @PostRun
    public void postRun() throws Exception {
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


    /**
     * Retrieves sut endpoint address from deployment manager and sets resolved address as field of this class
     */
    private void setSutEndpoint() throws Exception {

        StringBuilder urlBuilder = new StringBuilder();
        String targetServiceName = runXml.getXPathValue("sutConfiguration/serviceName");
        String targetServiceEndpoint = runXml.getXPathValue("sutConfiguration/endpoint");
        String privatePort = runXml.getXPathValue("benchFlowServices/privatePort");

        String targetServiceAddress = BenchmarkUtils.benchFlowServiceAddress(
                                                        deploymentManagerAddress,
                                                        privatePort,
                                                        targetServiceName,
                                                        trialId,
                                                        http.getHttpClient());

        sutEndpoint = urlBuilder.append("http://")
                .append(targetServiceAddress)
                .append(targetServiceEndpoint).toString();

        runXml.addConfigurationNode("sutConfiguration", "sutEndpoint", sutEndpoint);
    }


    /**
     * Setup benchmarkDir, http transport, and services info map
     */
    protected void initialize() throws Exception {
        this.benchmarkDir = Paths.get(RunContext.getBenchmarkDir());
        this.http = new ApacheHC3Transport();
        this.runXml = new RunXml(params);
        moveBenchFlowServicesConfigToProperties();
    }

    /**
     * Moves benchFlowServices section of run.xml to first driver properties
     */
    protected void moveBenchFlowServicesConfigToProperties() throws Exception {

        Document runDoc = params.getTopLevelElements().item(0).getOwnerDocument();
        String driverConfigNodeXPath = "fa:runConfig/fd:driverConfig[1]";
        Element driverConfigNode = (Element) runXml.getNode(driverConfigNodeXPath);
        Element driverProperties = (Element) runXml.getNode("properties", driverConfigNode);
        Element benchFlowServicesConfiguration = (Element) runXml.getNode("benchFlowServices");

        if (driverProperties == null) {
            logger.info("Adding properties node for driver WfMSStartDriver");
            driverProperties = runXml.addConfigurationNode(driverConfigNodeXPath, "properties", "");
        }

        boolean omitDeclaration = false;
        boolean prettyPrint = true;

        //serialize benchFlowServices node to string
        String serializedBenchFlowServicesConfiguration =
                BenchmarkUtils.nodeToString(benchFlowServicesConfiguration, omitDeclaration, prettyPrint);

        //escape it
        serializedBenchFlowServicesConfiguration =
                StringEscapeUtils.escapeXml11(serializedBenchFlowServicesConfiguration);

        //add a property node to properties to save the serialized configuration
        Element benchFlowServicesProperty = runXml.addConfigurationNode(driverProperties, "property", "");

        //set the serialized configuration as value of the property node
        benchFlowServicesProperty.setAttribute("name", "benchFlowServices");
        benchFlowServicesProperty.appendChild(runDoc.createTextNode(serializedBenchFlowServicesConfiguration));
        driverProperties.appendChild(benchFlowServicesProperty);
        runXml.save();

    }

}