package cloud.benchflow.driversmaker.modules;

import cloud.benchflow.driversmaker.configurations.DriversMakerConfiguration;
import cloud.benchflow.driversmaker.utils.BenchFlowEnv;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.io.FileNotFoundException;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 13/02/16.
 */
public class BenchFlowEnvModule extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides @Singleton
    @Named("bfEnv")
    public BenchFlowEnv providesBenchFlowEnv(DriversMakerConfiguration dmc) throws FileNotFoundException {
        String config = dmc.getBenchFlowEnvConfiguration().getConfigPath();
        String bfServices = dmc.getBenchFlowEnvConfiguration().getBenchFlowServicesPath();
        String bfCompose = dmc.getBenchFlowEnvConfiguration().getBenchFlowComposeAddress();
        String skeleton = dmc.getBenchFlowEnvConfiguration().getDriverSkeletonPath();
        return new BenchFlowEnv(config, bfServices, bfCompose, skeleton);
    }

}
