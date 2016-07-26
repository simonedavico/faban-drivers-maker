package cloud.benchflow.driversmaker.generation;

import cloud.benchflow.driversmaker.generation.benchflowservices.*;
import cloud.benchflow.driversmaker.generation.utils.BenchmarkUtils;

import com.sun.faban.driver.*;
import com.sun.faban.driver.transport.hc3.ApacheHC3Transport;

import org.apache.commons.lang3.StringEscapeUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;
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
    private String deploymentManagerAddress;
    private BenchFlowServices benchFlowServices;
    private String privatePort;

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



    @OnceBefore
    public void onceBefore() throws Exception {
        //We wait a bit to create a gap in the data (TODO-RM: experimenting with data cleaning)
        //and be sure the model started during the warm up and timing synch of the sistem, end,
        //event though now that we use mock models they end very fast
        Thread.sleep(20000);
        logger.info("Tested pre-run (sleep 20) done");
        benchFlowServices.start();
    }


    //if monitor == end -> start, monitor, stop
    //if monitor == all -> monitor, stop
    //collector -> stop
    @OnceAfter
    public void onceAfter() throws Exception {
        benchFlowServices.stop();
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

        trialId = getXPathValue("benchFlowRunConfiguration/trialId");
        deploymentManagerAddress = getXPathValue("benchFlowServices/deploymentManager");
        privatePort = getXPathValue("benchFlowServices/privatePort");

        logger = ctx.getLogger();

        Map<String, ServiceInfo> serviceInfoMap = parseBenchmarkConfiguration();
        benchFlowServices = new BenchFlowServices(serviceInfoMap,
                                                  deploymentManagerAddress,
                                                  privatePort,
                                                  trialId,
                                                  http.getHttpClient(),
                                                  logger);
    }

    /**
     * Parse run.xml to build a service info map
     */
    private Map<String, ServiceInfo> parseBenchmarkConfiguration() throws Exception {

        Map<String, ServiceInfo> serviceInfoMap = new HashMap<>();

        logger.info("About to parse serialized BenchFlow services configuration");

        String benchFlowServicesSerializedNode = getContextProperty("benchFlowServices");
        benchFlowServicesSerializedNode = StringEscapeUtils.unescapeXml(benchFlowServicesSerializedNode);
        Node benchFlowServices = BenchmarkUtils.stringToNode(benchFlowServicesSerializedNode);

        logger.info("Successfully parsed BenchFlow services configuration");

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
        return serviceInfoMap;
    }

}
