package cloud.benchflow.driversmaker.utils.env;

import cloud.benchflow.benchmark.heuristics.GenerationDefaults;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 03/03/16.
 */
public class DriversMakerEnv /*extends BenchFlowEnv*/ {

    private String benchFlowServicesPath;
    private String generationResourcesPath;
    private BenchFlowEnv configYml;
    private GenerationDefaults heuristics;


    public DriversMakerEnv(/*String configPath,*/
                           BenchFlowEnv configYml,
                           String benchFlowServicesPath,
                           String generationResourcesPath) {
        //super(configPath);
        this.configYml = configYml;
        this.benchFlowServicesPath = benchFlowServicesPath;
        this.generationResourcesPath = generationResourcesPath;
        this.heuristics = new GenerationDefaults(configYml);
    }

    public String getBenchFlowServicesPath() {
        return benchFlowServicesPath;
    }

    public BenchFlowEnv getConfigYml() {
        return this.configYml;
    }

    public GenerationDefaults getHeuristics() {
        return this.heuristics;
    }

    public String getBenchFlowComposeAddress() {
        return this.configYml.<String>getVariable("BENCHFLOW_COMPOSE_ADDRESS");
        //return this.<String>getVariable("BENCHFLOW_COMPOSE_ADDRESS");
//        return benchFlowComposeAddress;
    }

    public String getEnvConsulAddress() {
        return this.configYml.<String>getVariable("BENCHFLOW_ENVCONSUL_CONSUL_ADDRESS");
    }

    public String getGenerationResourcesPath() {
        return generationResourcesPath;
    }

    public void setGenerationResourcesPath(String generationResourcesPath) {
        this.generationResourcesPath = generationResourcesPath;
    }

}