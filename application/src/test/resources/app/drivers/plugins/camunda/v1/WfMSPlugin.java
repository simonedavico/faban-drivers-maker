package cloud.benchflow.plugins.camunda.v1;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cloud.benchflow.libraries.WfMSApi;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import java.io.File;
import java.io.IOException;

private class WfMSPlugin extends WfMSApi {

    protected String processDefinitionAPI;
    private static String MY_SQL_MONITOR_QUERY = "...";

    public WfMSPlugin(String sutEndpoint) {
        super(sutEndpoint, "/deployment/create");
        this.processDefinitionAPI = sutEndpoint + "/process-definition";
    }

    @Override
    public Map<String, String> deploy(File model) throws IOException {

        Map<String, String> result = new HashMap<String, String>();
        StringPart deploymentName = new StringPart("deployment-name", model.getName());
        List<Part> parts = new ArrayList<Part>();

        FilePart process = new FilePart("*", model);

        parts.add(deploymentName);
        parts.add(process);
        StringBuilder deployDef = http.fetchURL(deployAPI, parts);

        JsonObject deployObj = parser.parse(deployDef.toString()).getAsJsonObject();
        String deploymentId = deployObj.get("id").getAsString();

        //Obtain process definition data
        StringBuilder procDef = http.fetchURL(processDefinitionAPI + "?deploymentId=" + deploymentId);
        String processDefinitionResponse = procDef.toString();

        JsonArray procDefArray = parser.parse(processDefinitionResponse).getAsJsonArray();
        //We only get 1 element using the deploymentId
        String processDefinitionId = procDefArray.get(0).getAsJsonObject().get("id").getAsString();
        result.put(model.getName(), processDefinitionId);
        return result;

    }
}