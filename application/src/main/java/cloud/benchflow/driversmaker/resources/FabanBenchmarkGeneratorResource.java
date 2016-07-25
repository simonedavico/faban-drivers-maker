package cloud.benchflow.driversmaker.resources;

import cloud.benchflow.experiment.BenchmarkGenerator;
import cloud.benchflow.driversmaker.exceptions.BenchmarkGenerationException;
import cloud.benchflow.driversmaker.requests.Experiment;
import cloud.benchflow.driversmaker.requests.Trial;
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv;
import cloud.benchflow.driversmaker.utils.minio.BenchFlowMinioClient;

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

import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;


/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 25/02/16.
 */
@javax.ws.rs.Path("generatedriver")
public class FabanBenchmarkGeneratorResource {

    private DriversMakerEnv benv;
    private BenchFlowMinioClient minio;
    private Logger logger;

    @Inject
    public FabanBenchmarkGeneratorResource(@Named("generationEnv") DriversMakerEnv benv,
                                           @Named("minio") BenchFlowMinioClient minio) {
        this.benv = benv;
        this.minio = minio;
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
    }

    private void buildDriver(final Path driverPath, final String experimentId) {
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

    //copies classes from package cloud.benchflow.driversmaker.generation
    //into the benchmark skeleton
    private void copyGenerationSources(final Path benchmarkPath) throws IOException {

        Path generationSourcesPath = Paths.get("./application/src/main/java/cloud/benchflow/driversmaker/generation");
        FileUtils.copyDirectory(generationSourcesPath.toFile(),
                                benchmarkPath.resolve("src/cloud/benchflow/driversmaker/generation").toFile());

    }

    @Deprecated
    private void changeBenchmarkName(final java.nio.file.Path driverPath, final String experimentId) throws IOException {
        java.nio.file.Path driverClassPath =
                driverPath.resolve("src/cloud/benchflow/wfmsbenchmark/driver/WfMSBenchmarkDriver.java");
        String src = FileUtils.readFileToString(driverClassPath.toFile(), Charsets.UTF_8);
        src = src.replaceFirst("WfMSBenchmark Workload", "[" + experimentId + "] WfMSBenchmark Workload");
        FileUtils.writeStringToFile(driverClassPath.toFile(), src, Charsets.UTF_8);
    }

    private void cleanUp(Experiment experiment) {
        String minioBenchmarkId = experiment.getUserId() + "/" + experiment.getBenchmarkName();
        long experimentNumber = experiment.getExperimentNumber();
        Iterator<Trial> trials = experiment.getAllTrials();
        minio.removeBenchFlowBenchmark(minioBenchmarkId, experimentNumber);
        while(trials.hasNext()) {
            Trial trial = trials.next();
            int trialNumber = trial.getTrialNumber();
            minio.removeDeploymentDescriptor(minioBenchmarkId, experimentNumber, trialNumber);
            minio.removeFabanConfiguration(minioBenchmarkId, experimentNumber, trialNumber);
        }
    }

    @POST
    public Response generateBenchmark(Experiment experiment) throws IOException {
        //temporary user id
        experiment.setUserId("BenchFlow");
        String benchmarkId = experiment.getBenchmarkId();
        String minioBenchmarkId = experiment.getUserId() + "/" + experiment.getBenchmarkName();

        String experimentId = experiment.getExperimentId();
        String minioExperimentId = experimentId.replace(".", "/");
        long experimentNumber = experiment.getExperimentNumber();

        //temporary: get zip of sources and copy in temporary folder
//        InputStream sources = minio.getBenchmarkSources(minioBenchmarkId);
//        logger.debug("Retrieved driver sources from minio");

        try(ManagedDirectory managedDriverPath = new ManagedDirectory(
                "./tmp/" + benchmarkId + "/" + experiment.getExperimentId())
            ) {

            Path driverPath = managedDriverPath.getPath();
//            FileUtils.forceMkdir(driverPath.toFile());
//            ZipUtil.unwrap(sources, driverPath.toFile());

            //copy skeleton in temporary folder
//            Path generationResources = Paths.get("./application/src/test/resources/app/drivers");
            Path generationResources = Paths.get(benv.getGenerationResourcesPath());
            Path skeletonPath = generationResources.resolve("templates/skeleton/benchmark");
            FileUtils.copyDirectory(skeletonPath.toFile(), driverPath.toFile());

            logger.debug("About to generate benchmark for experiment " + experiment.getExperimentId());

            Iterator<Trial> trials = experiment.getAllTrials();
//            String deploymentDescriptor = minio.getOriginalDeploymentDescriptor(minioBenchmarkId);
            String deploymentDescriptor = minio.getDeploymentDescriptorForExperiment(minioBenchmarkId, experimentNumber);
//            String benchmarkConfiguration = minio.getOriginalBenchFlowBenchmark(minioBenchmarkId);
            String benchmarkConfiguration = minio.getBenchFlowBenchmarkForExperiment(minioBenchmarkId, experimentNumber);

            Path descriptorsPath = driverPath.resolve("build/sut/");

            Path generatedBenchmarkOutputDir = driverPath;//.resolve("src");

            BenchmarkGenerator benchmarkGenerator =
                    new BenchmarkGenerator(
                            experiment.getExperimentId(),
                            benchmarkConfiguration,
                            deploymentDescriptor,
                            generatedBenchmarkOutputDir,
                            benv);

            benchmarkGenerator.generateSources();
            logger.debug("Generated drivers sources");

            while(trials.hasNext()) {

                Trial trial = trials.next();
                String dd = benchmarkGenerator.generateDeploymentDescriptorForTrial(trial);
                String runXml = benchmarkGenerator.generateFabanConfigurationForTrial(trial);

                logger.debug("Generated deployment descriptor and configuration for trial " + trial.getTrialId());

                Path descriptorPath = descriptorsPath.resolve("docker-compose-" + trial.getTrialId() + ".yml");
                FileUtils.writeStringToFile(descriptorPath.toFile(), dd, Charsets.UTF_8);

                minio.saveDeploymentDescriptor(minioBenchmarkId, experimentNumber, trial.getTrialNumber(), dd);
                minio.saveFabanConfiguration(minioBenchmarkId, experimentNumber, trial.getTrialNumber(), runXml);

                logger.debug("Saved on minio deployment descriptor and configuration for trial " + trial.getTrialId());
            }

            //this should have no effect for http drivers
            Path modelsPath = driverPath.resolve("build/models");
            List<String> models = minio.listModels(benchmarkId);
            for(String modelName : models) {
                String model = minio.getModel(benchmarkId, modelName);
                Path modelPath = modelsPath.resolve(modelName);
                FileUtils.writeStringToFile(modelPath.toFile(), model, Charsets.UTF_8);
                logger.debug("Retrieved model " + modelName);
            }

            logger.debug("About to build generated driver");

            copyGenerationSources(driverPath);
            buildDriver(driverPath, experiment.getExperimentId());

            //save generated driver to minio
            Path generatedDriverPath = driverPath.resolve("build/" + experiment.getExperimentId() + ".jar");
            minio.saveGeneratedDriver(minioBenchmarkId, experimentNumber, generatedDriverPath.toAbsolutePath().toString());

            logger.debug("Successfully saved generated driver on Minio");

        } catch (Exception e) {
            cleanUp(experiment);
            throw new BenchmarkGenerationException(e.getMessage(), e);
        }

        return Response.ok().build();
    }


}
