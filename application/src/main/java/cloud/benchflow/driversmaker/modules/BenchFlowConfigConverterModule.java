package cloud.benchflow.driversmaker.modules;

import cloud.benchflow.config.converter.BenchFlowBenchmarkConfigConverter;
import cloud.benchflow.driversmaker.configurations.DriversMakerConfiguration;
import cloud.benchflow.driversmaker.configurations.FabanDefaults;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 05/02/16.
 */
public class BenchFlowConfigConverterModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    @Named("bfbenchmark")
    public BenchFlowBenchmarkConfigConverter
           provideBenchflowBenchmarkConfigConverter(DriversMakerConfiguration dmc) {
        FabanDefaults fc = dmc.getFabanDefaults();
        return new BenchFlowBenchmarkConfigConverter(fc.getJavaHome(), fc.getJavaOpts());
    }

}
