package cloud.benchflow.driversmaker.modules;

import cloud.benchflow.driversmaker.configurations.DriversMakerConfiguration;
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv;
import cloud.benchflow.driversmaker.utils.minio.BenchFlowMinioClient;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 25/02/16.
 */
public class MinioModule extends AbstractModule {


    @Override
    protected void configure() {}

    @Provides
    @Singleton
    @Named("minio")
    @Inject
    public BenchFlowMinioClient provideMinio(DriversMakerConfiguration dc,
                                             @Named("generationEnv")DriversMakerEnv generationEnv)
            throws InvalidPortException, InvalidEndpointException {

        String minioAddress = dc.getMinioConfiguration().getAddress();
        String accessKey = generationEnv.getConfigYml().<String>getVariable("BENCHFLOW_MINIO_ACCESS_KEY");
        String secretKey = generationEnv.getConfigYml().<String>getVariable("BENCHFLOW_MINIO_SECRET_KEY");
        return new BenchFlowMinioClient(minioAddress,accessKey,secretKey);
    }
}
