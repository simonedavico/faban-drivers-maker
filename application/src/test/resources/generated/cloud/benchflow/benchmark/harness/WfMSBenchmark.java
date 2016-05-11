package cloud.benchflow.benchmark.harness;


public class WfMSBenchmark extends com.sun.faban.harness.DefaultFabanBenchmark2 {
    private cloud.benchflow.libraries.WfMSApi plugin = null;

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

    @java.lang.Override
    @com.sun.faban.harness.Validate
    public void validate() throws java.lang.Exception {
        initialize();
    }

    private void initialize() {
        plugin = new WfMSPlugin(sutEndpoint);
    }

    private class WfMSPlugin extends cloud.benchflow.benchmark.harness.WfMSBenchmark.WfMSApi {
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
}

