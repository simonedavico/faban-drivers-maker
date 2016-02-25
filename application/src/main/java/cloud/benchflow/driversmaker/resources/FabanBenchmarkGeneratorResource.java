package cloud.benchflow.driversmaker.resources;

import cloud.benchflow.driversmaker.configurations.FabanDefaults;
import cloud.benchflow.driversmaker.requests.Trial;
import cloud.benchflow.driversmaker.utils.BenchFlowEnv;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 25/02/16.
 */
@Path("makedriver")
public class FabanBenchmarkGeneratorResource {

    private BenchFlowEnv benv;
    private FabanDefaults fabanDefaults;

    @Inject
    public FabanBenchmarkGeneratorResource(@Named("bfEnv") BenchFlowEnv benv,
                                           @Named("fabanDefaults") FabanDefaults defaults) {
        this.benv = benv;
        this.fabanDefaults = defaults;
    }

    @POST
    public String generateBenchmark(Trial trial) {
        return "TODO: Generate benchmark for trial";
    }


}
