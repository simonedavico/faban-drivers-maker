package cloud.benchflow.driversmaker.generation.benchflowservices;

import cloud.benchflow.driversmaker.generation.utils.BenchmarkUtils;
import cloud.benchflow.monitors.Monitor;
import cloud.benchflow.monitors.MonitorFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 26/07/16.
 */
public class BenchFlowServices {

    private Map<String, ServiceInfo> serviceInfoMap;
    private String deploymentManagerAddress;
    private String privatePort;
    private String trialId;
    private HttpClient http;
    private Logger logger;


    public BenchFlowServices(Map<String, ServiceInfo> serviceInfoMap,
                             String deploymentManagerAddress,
                             String privatePort,
                             String trialId,
                             HttpClient http,
                             Logger logger) {
        this.serviceInfoMap = serviceInfoMap;
        this.deploymentManagerAddress = deploymentManagerAddress;
        this.privatePort = privatePort;
        this.trialId = trialId;
        this.http = http;
        this.logger = logger;
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

    private Monitor getMonitor(MonitorInfo monitorInfo) throws Exception {
        String monitorEndpoint = BenchmarkUtils.benchFlowServiceAddress(
                deploymentManagerAddress,
                privatePort,
                monitorInfo.getId(),
                trialId,
                http);

        return MonitorFactory.getMonitor(
                monitorInfo.getName(),
                monitorInfo.getParams(),
                monitorEndpoint,
                monitorInfo.getStartAPI(),
                monitorInfo.getStopAPI(),
                monitorInfo.getMonitorAPI(),
                logger);
    }


    public void start() throws Exception {

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

                try {
                    Monitor monitor = getMonitor(monitorInfo);

                    if(monitorInfo.getRunPhase().equals("start")) {
                        monitor.run();
                    } else if(monitorInfo.getRunPhase().equals("all")) {
                        monitor.start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        };

        //if startApi -> start
        Consumer<CollectorInfo> collectorInfoConsumer = new Consumer<CollectorInfo>() {
            @Override
            public void accept(CollectorInfo collectorInfo) {

                if(collectorInfo.getStartAPI() != null) {

                    try {
                        String collectorEndpoint = BenchmarkUtils.benchFlowServiceAddress(
                                deploymentManagerAddress,
                                privatePort,
                                collectorInfo.getId(),
                                trialId,
                                http);


                        HttpMethod post = new PostMethod(collectorEndpoint + collectorInfo.getStartAPI());
                        http.executeMethod(post);

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

    public void stop() {

        Consumer<ServiceInfo> serviceInfoConsumer = new Consumer<ServiceInfo>() {
            @Override
            public void accept(ServiceInfo serviceInfo) {
                //do nothing
            }
        };

        Consumer<MonitorInfo> monitorInfoConsumer = new Consumer<MonitorInfo>() {
            @Override
            public void accept(MonitorInfo monitorInfo) {

                try {

                    Monitor monitor = getMonitor(monitorInfo);

                    if(monitorInfo.getRunPhase().equals("end")) {
                        monitor.run();
                    } else if(monitorInfo.getRunPhase().equals("all")) {
                        monitor.monitor();
                        monitor.stop();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
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

                    HttpMethod put = new PutMethod(collectorEndpoint + collectorInfo.getStopAPI());
                    http.executeMethod(put);

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
}
