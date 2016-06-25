package cloud.benchflow.libraries;

import java.util.Map;

import java.io.File;
import java.io.IOException;

public abstract class WfMSApi {

    protected String sutEndpoint;
    protected String deployAPI;

    public WfMSApi(String se, String d) {
        sutEndpoint = se;
        deployAPI = sutEndpoint + d;
    }

    public abstract Map<String, String> deploy(File model) throws IOException;

    public abstract String startProcessInstance(String processDefinitionId, String data) throws IOException;

}