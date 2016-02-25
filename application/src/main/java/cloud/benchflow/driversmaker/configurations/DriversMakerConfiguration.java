package cloud.benchflow.driversmaker.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 06/01/16.
 */
public class DriversMakerConfiguration extends Configuration {

    @Valid
    @NotNull
    private FabanDefaults fabanDefaults = new FabanDefaults();

    @Valid
    @NotNull
    private BenchFlowEnvConfiguration benchFlowEnvConfiguration = new BenchFlowEnvConfiguration();

    @JsonProperty("faban.defaults")
    public FabanDefaults getFabanDefaults() { return fabanDefaults; }

    @JsonProperty("faban.defaults")
    public void setFabanDefaults(FabanDefaults fc) { this.fabanDefaults = fc; }

    @JsonProperty("benchflow.env")
    public BenchFlowEnvConfiguration getBenchFlowEnvConfiguration() {
        return benchFlowEnvConfiguration;
    }

    @JsonProperty("benchflow.env")
    public void setBenchFlowEnvConfiguration(BenchFlowEnvConfiguration benchFlowEnvConfiguration) {
        this.benchFlowEnvConfiguration = benchFlowEnvConfiguration;
    }
}
