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

    @JsonProperty("benchflowServices")
    public String getBenchFlowServicesPath() {
        return benchFlowServicesPath;
    }

    @JsonProperty("benchflowServices")
    public void setBenchFlowServicesPath(String benchFlowServicesPath) {
        this.benchFlowServicesPath = benchFlowServicesPath;
    }

    @JsonProperty("benchflowCompose")
    public String getBenchFlowComposeAddress() {
        return benchFlowComposeAddress;
    }

    @JsonProperty("benchflowCompose")
    public void setBenchFlowComposeAddress(String benchFlowComposeAddress) {
        this.benchFlowComposeAddress = benchFlowComposeAddress;
    }

    @JsonProperty("driverSkeleton")
    public String getDriverSkeletonPath() {
        return driverSkeletonPath;
    }

    @JsonProperty("driverSkeleton")
    public void setDriverSkeletonPath(String driverSkeletonPath) {
        this.driverSkeletonPath = driverSkeletonPath;
    }
}
