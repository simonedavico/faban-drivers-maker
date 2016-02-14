package cloud.benchflow.driversmaker.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 13/02/16.
 */
public class BenchFlowEnvConfiguration {

    @NotEmpty
    private String configPath;

    @NotEmpty
    private String benchFlowServicesPath;

    @JsonProperty("config.yml")
    public String getConfigPath() {
        return configPath;
    }

    @JsonProperty("config.yml")
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    @JsonProperty("benchflow.services")
    public String getBenchFlowServicesPath() {
        return benchFlowServicesPath;
    }

    @JsonProperty("benchflow.services")
    public void setBenchFlowServicesPath(String benchFlowServicesPath) {
        this.benchFlowServicesPath = benchFlowServicesPath;
    }
}
