package cloud.benchflow.driversmaker.modules;

import cloud.benchflow.driversmaker.configurations.DriversMakerConfiguration;
import cloud.benchflow.driversmaker.configurations.FabanDefaults;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 25/02/16.
 */
public class FabanDefaultsModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides @Named("fabanDefaults")
    public FabanDefaults provideFabanDefaults(DriversMakerConfiguration dc) {
        return dc.getFabanDefaults();
    }

}
