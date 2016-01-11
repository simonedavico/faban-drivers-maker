package cloud.benchflow.driversmaker;


import cloud.benchflow.config.converter.BenchFlowConfigConverter;
import cloud.benchflow.driversmaker.configurations.DriversMakerConfiguration;
import cloud.benchflow.driversmaker.resources.FabanConfigResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 06/01/16.
 */
public class DriversMakerApplication extends Application<DriversMakerConfiguration> {

    public static void main(String[] args) throws Exception {
        new DriversMakerApplication().run(args);
    }

    @Override
    public String getName() {
        return "drivers-maker";
    }

    @Override
    public void initialize(Bootstrap<DriversMakerConfiguration> bootstrap) {
        // nothing to do yet
    }

    @Override
    public void run(DriversMakerConfiguration driversMakerConfiguration, Environment environment) throws Exception {

        final String javaHome = driversMakerConfiguration.getFabanConfiguration().getJavaHome();
        final String javaOpts = driversMakerConfiguration.getFabanConfiguration().getJavaOpts();

        BenchFlowConfigConverter bfc = new BenchFlowConfigConverter(javaHome, javaOpts);
        final FabanConfigResource configConverter = new FabanConfigResource(bfc);

        environment.jersey().register(MultiPartFeature.class);
        environment.jersey().register(configConverter);

    }
}
