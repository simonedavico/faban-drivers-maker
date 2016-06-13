package cloud.benchflow.driversmaker.resources;

import cloud.benchflow.driversmaker.requests.Experiment;
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv;
import cloud.benchflow.driversmaker.utils.minio.BenchFlowMinioClient;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;


import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 29/02/16.
 */
@Path("testfileutils")
public class FileUtilsTestResource {

    private DriversMakerEnv env;
    private BenchFlowMinioClient minio;

    @Inject
    public FileUtilsTestResource(@Named("bfEnv") DriversMakerEnv env,
                                 @Named("minio") BenchFlowMinioClient minio) {
        this.env = env;
        this.minio = minio;
    }

    @POST
    public String test(Experiment experiment) throws IOException {
        experiment.setUserId("BenchFlow");
        String benchmarkId = experiment.getBenchmarkId();
        long experimentNumber = experiment.getExperimentNumber();

        InputStream sources = minio.getBenchmarkSources(benchmarkId);
//        String tmpPath = "./tmp/" + benchmarkId + "/" + experiment.getExperimentId();
//        String tmpPath = "./tmp/" + benchmarkId + "/foo/";
        java.nio.file.Path tmpPath = Paths.get("./tmp/" + benchmarkId + "/" + experiment.getExperimentId());
        FileUtils.forceMkdir(tmpPath.toFile());
        ZipUtil.unwrap(sources, tmpPath.toFile());
        return "COMPLETED";
    }
}
