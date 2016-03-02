package cloud.benchflow.driversmaker.resources;

import cloud.benchflow.config.BenchFlowBenchmarkConfigurationBuilder;
import cloud.benchflow.driversmaker.configurations.FabanDefaults;
import cloud.benchflow.driversmaker.requests.Experiment;
import cloud.benchflow.driversmaker.requests.Trial;
import cloud.benchflow.driversmaker.utils.BenchFlowEnv;
import cloud.benchflow.driversmaker.utils.BenchFlowMinioClient;

import cloud.benchflow.driversmaker.utils.ManagedDirectory;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.zeroturnaround.zip.ZipUtil;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;


/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 25/02/16.
 */
@Path("makedriver")
public class FabanBenchmarkGeneratorResource {

    private BenchFlowEnv benv;
    private FabanDefaults fabanDefaults;
    private BenchFlowMinioClient minio;
    private Logger logger;

    @Inject
    public FabanBenchmarkGeneratorResource(@Named("bfEnv") BenchFlowEnv benv,
                                           @Named("fabanDefaults") FabanDefaults defaults,
                                           @Named("minio") BenchFlowMinioClient minio) {
        this.benv = benv;
        this.fabanDefaults = defaults;
        this.minio = minio;
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
    }

    private void buildDriver(final java.nio.file.Path driverPath, final String experimentId) {
        Project p = new Project();
        p.setUserProperty("ant.file", driverPath.resolve("build.xml").toAbsolutePath().toString());
        p.setProperty("bench.shortname", experimentId);
        p.setProperty("faban.home", "faban/stage");
        p.init();
        ProjectHelper helper = ProjectHelper.getProjectHelper();
        p.addReference("ant.projectHelper", helper);
        helper.parse(p, driverPath.resolve("build.xml").toFile());
        p.executeTarget("build");
    }

    private void changeBenchmarkName(final java.nio.file.Path driverPath, final String experimentId) throws IOException {
        java.nio.file.Path driverClassPath =
                driverPath.resolve("src/cloud/benchflow/wfmsbenchmark/driver/WfMSBenchmarkDriver.java");
        String src = FileUtils.readFileToString(driverClassPath.toFile(), Charsets.UTF_8);
        src = src.replaceFirst("WfMSBenchmark Workload", "[" + experimentId + "] WfMSBenchmark Workload");
        FileUtils.writeStringToFile(driverClassPath.toFile(), src, Charsets.UTF_8);
    }

    @POST
    public Response generateBenchmark(Experiment experiment) throws IOException {
        //temporary user id
        experiment.setUserId("BenchFlow");
        String benchmarkId = experiment.getBenchmarkId();
        int experimentNumber = experiment.getExperimentNumber();

        //temporary: get zip of sources and copy in temporary folder
        InputStream sources = minio.getBenchmarkSources(benchmarkId);
        logger.debug("Retrieved driver sources from minio");

        try(ManagedDirectory managedDriverPath =
                    new ManagedDirectory("./tmp/" + benchmarkId + "/" + experiment.getExperimentId())) {

            java.nio.file.Path driverPath = managedDriverPath.getPath();
            FileUtils.forceMkdir(driverPath.toFile());
            ZipUtil.unwrap(sources, driverPath.toFile());

            logger.debug("About to generate benchmark for experiment " + experiment.getExperimentId());

            Iterator<Trial> trials = experiment.getAllTrials();
            String defaultDeploymentDescriptor = minio.getOriginalDeploymentDescriptor(benchmarkId);
            String defaultBenchmarkConfiguration = minio.getOriginalBenchFlowBenchmark(benchmarkId);

            java.nio.file.Path descriptorsPath = driverPath.resolve("build/sut/");

            BenchFlowBenchmarkConfigurationBuilder builder =
                    new BenchFlowBenchmarkConfigurationBuilder(defaultBenchmarkConfiguration,
                            defaultDeploymentDescriptor, benv, fabanDefaults);

            while(trials.hasNext()) {

                Trial trial = trials.next();
                String dd = builder.buildDeploymentDescriptor(trial);
                String runXml = builder.buildFabanBenchmarkConfiguration(trial);

                logger.debug("Generated deployment descriptor and configuration for trial " + trial.getTrialId());

                java.nio.file.Path descriptorPath = descriptorsPath.resolve("docker-compose" + trial.getTrialId() + ".yml");
                FileUtils.writeStringToFile(descriptorPath.toFile(), dd, Charsets.UTF_8);

                minio.saveDeploymentDescriptor(benchmarkId, experimentNumber, trial.getTrialNumber(), dd);
                minio.saveFabanConfiguration(benchmarkId, experimentNumber, trial.getTrialNumber(), runXml);

                logger.debug("Saved on minio deployment descriptor and configuration for trial " + trial.getTrialId());
            }

            logger.debug("About to build generated driver");

            //temporary: change benchmark name in driver class
            changeBenchmarkName(driverPath, experiment.getExperimentId());

            //TODO: catch BuildException
            buildDriver(driverPath, experiment.getExperimentId());

            //save generated driver to minio
            java.nio.file.Path generatedDriverPath = driverPath.resolve("build/" + experiment.getExperimentId() + ".jar");
            minio.saveGeneratedDriver(benchmarkId, experimentNumber, generatedDriverPath.toAbsolutePath().toString());

            logger.debug("Successfully saved generated driver on Minio");

        }

        return Response.ok().build();
    }


}
