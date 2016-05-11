package cloud.benchflow.driversmaker.utils.env;

import java.io.FileNotFoundException;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 03/03/16.
 */
public class DriversMakerBenchFlowEnv extends BenchFlowEnv {

    private String benchFlowServicesPath;
    private String generationResourcesPath;

    public DriversMakerBenchFlowEnv(String configPath,
                                    String benchFlowServicesPath,
                                    String generationResourcesPath) throws FileNotFoundException {
        super(configPath);
        this.benchFlowServicesPath = benchFlowServicesPath;
        this.generationResourcesPath = generationResourcesPath;
    }

    public String getBenchFlowServicesPath() {
        return benchFlowServicesPath;
    }

    public String getBenchFlowComposeAddress() {
        return this.<String>getVariable("BENCHFLOW_COMPOSE_ADDRESS");
//        return benchFlowComposeAddress;
    }


    public String getGenerationResourcesPath() {
        return generationResourcesPath;
    }

    public void setGenerationResourcesPath(String generationResourcesPath) {
        this.generationResourcesPath = generationResourcesPath;
    }

}
