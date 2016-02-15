package cloud.benchflow.driversmaker.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 13/02/16.
 */
public class BenchFlowEnv {

    private Map<String, Object> env;
    private String configPath;
    private String benchFlowServicesPath;

    public BenchFlowEnv(String configPath, String benchFlowServicesPath) throws FileNotFoundException {
        this.configPath = configPath;
        this.benchFlowServicesPath = benchFlowServicesPath;
        reload();
    }

    private Map<String, Object> loadFromFile() throws FileNotFoundException {
        return (Map) new Yaml().load(new FileInputStream(configPath));
    }

    /***
     *
     * @param variable the name of the variable
     * @param <T> the type that we want to retrieve the value as (currently List or String)
     */
    public <T> T getVariable(String variable) {
        return (T) env.get(variable);
    }

    public void reload() throws FileNotFoundException {
        this.env = loadFromFile();
    }

    public String getBenchFlowServicesPath() {
        return benchFlowServicesPath;
    }
}
