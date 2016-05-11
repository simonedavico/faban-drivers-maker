package cloud.benchflow.libraries;

import java.util.Map;

import java.io.File;
import java.io.IOException;

public abstract class WfMSApi {

    protected String sutEndpoint;
    protected String deployAPI;

    public WfMSApi(String sutEndpoint, String deployAPI) {
        this.sutEndpoint = sutEndpoint;
        this.deployAPI = sutEndpoint + deployAPI;
    }

    public abstract Map<String, String> deploy(File model) throws IOException;

    public abstract String startProcessDefinition(String processDefinitionId) throws IOException;

}