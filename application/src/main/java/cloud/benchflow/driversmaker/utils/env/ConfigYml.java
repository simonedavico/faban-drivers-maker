package cloud.benchflow.driversmaker.utils.env;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 13/02/16.
 */
@SuppressWarnings("unchecked")
public class ConfigYml {

    private Map<String, Object> env;
    private String configPath;

    public ConfigYml(String configPath) throws FileNotFoundException {
        this.configPath = configPath;
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
        Map<String, Object> parsedConfigYml = loadFromFile();

        //remote key that map to "" values
        parsedConfigYml.keySet().stream().filter(
                key -> parsedConfigYml.get(key).equals("")
        )
        .forEach(parsedConfigYml::remove);

        this.env = parsedConfigYml;
    }
}
