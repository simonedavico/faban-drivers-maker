package cloud.benchflow.driversmaker.utils.env;

import cloud.benchflow.experiment.heuristics.GenerationDefaults;

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
    private int privatePort;

    public DriversMakerEnv(/*String configPath,*/
                           BenchFlowEnv configYml,
                           String benchFlowServicesPath,
                           String generationResourcesPath,
                           int privatePort) {
        //super(configPath);
        this.configYml = configYml;
        this.benchFlowServicesPath = benchFlowServicesPath;
        this.generationResourcesPath = generationResourcesPath;
        this.heuristics = new GenerationDefaults(configYml);
        this.privatePort = privatePort;
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

    public String getDeploymentManagerAddress() {
        return this.configYml.<String>getVariable("BENCHFLOW_DEPLOYMENT_MANAGER_ADDRESS");
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

    public int getPrivatePort() {
        return privatePort;
    }

    public String getPublicIp(String serverAlias) {
        return configYml.getVariable("BENCHFLOW_SERVER_" + serverAlias.toUpperCase() + "_PUBLICIP");
    }

    public String getLocalIp(String serverAlias) {
        return configYml.getVariable("BENCHFLOW_SERVER_" + serverAlias.toUpperCase() + "_PRIVATEIP");
    }

    public String getIp(String serverAlias) {
        String privateIp = getLocalIp(serverAlias);
        if(privateIp == null) {
            return getPublicIp(serverAlias);
        }
        return privateIp;
    }
}