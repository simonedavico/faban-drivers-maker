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
    private FabanConfiguration fabanConfiguration = new FabanConfiguration();

    @JsonProperty("faban.config")
    public FabanConfiguration getFabanConfiguration() { return fabanConfiguration; }

    @JsonProperty("faban.config")
    public void setFabanConfiguration(FabanConfiguration fc) { this.fabanConfiguration = fc; }

}
