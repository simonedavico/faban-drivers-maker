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

    @NotEmpty
    private String benchFlowComposeAddress;

    @NotEmpty
    private String driverSkeletonPath;

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

    @JsonProperty("benchflow.compose")
    public String getBenchFlowComposeAddress() {
        return benchFlowComposeAddress;
    }

    @JsonProperty("benchflow.compose")
    public void setBenchFlowComposeAddress(String benchFlowComposeAddress) {
        this.benchFlowComposeAddress = benchFlowComposeAddress;
    }

    @JsonProperty("driver.skeleton")
    public String getDriverSkeletonPath() {
        return driverSkeletonPath;
    }

    @JsonProperty("driver.skeleton")
    public void setDriverSkeletonPath(String driverSkeletonPath) {
        this.driverSkeletonPath = driverSkeletonPath;
    }
}
