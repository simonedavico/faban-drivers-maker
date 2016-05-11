package cloud.benchflow.benchmark.drivers;


@com.sun.faban.driver.FixedTime(cycleDeviation = 5, cycleTime = 1000, cycleType = com.sun.faban.driver.CycleType.THINKTIME)
@com.sun.faban.driver.BenchmarkDriver(metric = "req/s", name = "WfMSStartDriver", opsUnit = "requests", percentiles = { "25" , "50" , "75" , "90" , "95" , "99.9" }, responseTimeUnit = java.util.concurrent.TimeUnit.NANOSECONDS, threadPerScale = 1)
@com.sun.faban.driver.BenchmarkDefinition(metric = "req/s", name = "[MyExperiment.1.1] MyBenchmark Workload", version = "0.1")
public class WfMSStartDriver {
    private cloud.benchflow.libraries.WfMSApi plugin = null;

    private void initialize() {
        plugin = new WfMSPlugin(sutEndpoint);
    }

    public abstract class WfMSApi {
        protected java.lang.String sutEndpoint;

        protected java.lang.String deployAPI;

        public WfMSApi(java.lang.String sutEndpoint ,java.lang.String deployAPI) {
            cloud.benchflow.libraries.WfMSApi.this.sutEndpoint = sutEndpoint;
            cloud.benchflow.libraries.WfMSApi.this.deployAPI = sutEndpoint + deployAPI;
        }

        public abstract java.util.Map<java.lang.String, java.lang.String> deploy(java.io.File model) throws java.io.IOException;

        public abstract java.lang.String startProcessDefinition(java.lang.String processDefinitionId) throws java.io.IOException;
    }

    private class WfMSPlugin extends cloud.benchflow.benchmark.drivers.WfMSStartDriver.WfMSApi {
        protected java.lang.String processDefinitionAPI;

        private static java.lang.String MY_SQL_MONITOR_QUERY = "...";

        public WfMSPlugin(java.lang.String sutEndpoint) {
            super(sutEndpoint, "/deployment/create");
            cloud.benchflow.plugins.camunda.v1.WfMSPlugin.this.processDefinitionAPI = sutEndpoint + "/process-definition";
        }

        @java.lang.Override
        public java.util.Map<java.lang.String, java.lang.String> deploy(java.io.File model) throws java.io.IOException {
            java.util.Map<java.lang.String, java.lang.String> result = new java.util.HashMap<java.lang.String, java.lang.String>();
            org.apache.commons.httpclient.methods.multipart.StringPart deploymentName = new org.apache.commons.httpclient.methods.multipart.StringPart("deployment-name" , model.getName());
            java.util.List<org.apache.commons.httpclient.methods.multipart.Part> parts = new java.util.ArrayList<org.apache.commons.httpclient.methods.multipart.Part>();
            org.apache.commons.httpclient.methods.multipart.FilePart process = new org.apache.commons.httpclient.methods.multipart.FilePart("*" , model);
            parts.add(deploymentName);
            parts.add(process);
            java.lang.StringBuilder deployDef = http.fetchURL(deployAPI, parts);
            com.google.gson.JsonObject deployObj = parser.parse(deployDef.toString()).getAsJsonObject();
            java.lang.String deploymentId = deployObj.get("id").getAsString();
            java.lang.StringBuilder procDef = http.fetchURL((((processDefinitionAPI) + "?deploymentId=") + deploymentId));
            java.lang.String processDefinitionResponse = procDef.toString();
            com.google.gson.JsonArray procDefArray = parser.parse(processDefinitionResponse).getAsJsonArray();
            java.lang.String processDefinitionId = procDefArray.get(0).getAsJsonObject().get("id").getAsString();
            result.put(model.getName(), processDefinitionId);
            return result;
        }
    }

    @com.sun.faban.driver.BenchmarkOperation(max90th = 20000, name = "myModel", timing = com.sun.faban.driver.Timing.AUTO)
    public void domyModelRequest() {
        if (isStarted())
            wfms.startProcessInstance("myModel");
        else
            wfms.startProcessInstance("mock.bpmn");
        
    }
}

