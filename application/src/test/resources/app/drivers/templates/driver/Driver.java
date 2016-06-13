package cloud.benchflow.benchmark.drivers;

//import com.sun.faban.common.*;
import com.sun.faban.driver.*;

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CountDownLatch;

public class Driver {

    private DriverContext ctx;
    private HttpTransport http;
    private String sutEndpoint;
    private Logger logger;
    private DriverConfig driverConfig;
    private Map<String,String> modelsStartID;

    //TODO: add with spoon..what?



    public Driver() throws Exception {
        initialize();
        setSutEndpoint();
        //added with spoon
        //loadModelsInfo();
    }

    private class DriverConfig {

        private String getContextProperty(String property){
            return ctx.getProperty(property);
        }

        private String getXPathValue(String xPathExpression) throws Exception {
            return ctx.getXPathValue(xPathExpression);
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


    @OnceBefore
    public void onceBefore() throws Exception {
        //We wait a bit to create a gap in the data (TODO-RM: experimenting with data cleaning)
        //and be sure the model started during the warm up and timing synch of the sistem, end,
        //event though now that we use mock models they end very fast
        Thread.sleep(20000);
        logger.info("Tested pre-run (sleep 20) done");
    }


    @OnceAfter
    //TODO: modify this
    public void onceAfter() {

        // Currently we just use the MySQL monitor, since the overhead is minimal
        // curl "http://192.168.41.105:9303/status?query=SELECT+COUNT(*)+FROM+ACT_HI_PROCINST+WHERE+END_TIME_+IS+NULL&value=0&method=equal"
        //TODO: check what actually happens if one of the following exception is thrown
        final CountDownLatch done = new CountDownLatch(1);

        //one thread for each monitor
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                sqlMonitor.run();
//            }
//        })

        new Thread(new Runnable() {
            @Override
            public void run() {
                //TODO: use setParams
                String mysqlMonitorEndpoint = "";
                //TODO: we for sure want to have a better way to get the same.
                //the point now is that it is not possible to throw an exception on the run method
                try {
                    //simone: change this
                    // mysqlMonitorEndpoint = getXPathValue("services/monitors/mysql");
                    mysqlMonitorEndpoint = driverConfig.getXPathValue("benchFlowServices/monitors/mysql");

                    logger.info("mysqlMonitorEndpoint: " + mysqlMonitorEndpoint);

                } catch (Exception ex) {
                    Thread t = Thread.currentThread();
                    t.getUncaughtExceptionHandler().uncaughtException(t, ex);
                    return;
                }

                String queryCall = "?query=SELECT+COUNT(*)+FROM+ACT_HI_PROCINST+WHERE+END_TIME_+IS+NULL&value=0&method=equal";
                //TODO: improve, the empty answer with equal seems no, the not empty seems yes in the current implementation
                String res = "";

                while (true) {
                    //TODO: we for sure want to have a better way to get the same.
                    //the point now is that it is not possible to throw an exception on the run method
                    try {
                        res = new BenchFlowServicesAsynchInteraction(mysqlMonitorEndpoint + queryCall).call();
                    } catch (Exception ex) {
                        Thread t = Thread.currentThread();
                        t.getUncaughtExceptionHandler().uncaughtException(t, ex);
                        return;
                    }

                    logger.info("Waiting for workload to complete, res: " + res);

                    //TODO: this is really custom given the current way the monitor works
                    if (res.toLowerCase().contains("matches 0")) {
                        break;
                    } else {
                        //Pause for 10 seconds
                        logger.info("Waiting for workload to complete, waiting to restart");
                        //TODO: we for sure want to have a better way to get the same.
                        //the point now is that it is not possible to throw an exception on the run method
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ex) {
                            Thread t = Thread.currentThread();
                            t.getUncaughtExceptionHandler().uncaughtException(t, ex);
                            return;
                        }
                    }
                }
                done.countDown();
            }
        }).start();
    }


    private void setSutEndpoint() throws Exception {
        this.sutEndpoint = driverConfig.getXPathValue("sutConfiguration/sutEndpoint");
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
        int numModel = Integer.parseInt(driverConfig.getContextProperty("model_num"));
        for (int i = 1; i <= numModel; i++) {
            String name = driverConfig.getContextProperty("model_" + i + "_name");
            String startID = driverConfig.getContextProperty("model_" + i + "_startID");
            this.modelsStartID.put(name, startID);
        }
    }

    private void initialize() {
        this.ctx = DriverContext.getContext();
        HttpTransport.setProvider("com.sun.faban.driver.transport.hc3.ApacheHC3Transport");
        this.http = HttpTransport.newInstance();
        this.logger = ctx.getLogger();
        this.driverConfig = new DriverConfig();
        this.modelsStartID = new HashMap<String,String>();
    }

}