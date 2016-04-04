package cloud.benchflow.driversmaker.utils.minio;

import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 26/02/16.
 */
@SuppressWarnings("unused")
public class BenchFlowMinioClient {

    private MinioClient mc;
    private static final String BENCHMARKS_BUCKET = "benchmarks";

    public BenchFlowMinioClient(final String address, final String accessKey, final String privateKey)
            throws InvalidPortException, InvalidEndpointException {
//        System.out.println("Access key: " + accessKey);
//        System.out.println("Secret key: " + privateKey);
//        System.out.println("Address: " + address);
        this.mc = new MinioClient(address, accessKey, privateKey);
    }

    //TODO: implement this?
    public void saveOriginalBenchmark(final String benchmarkId, final byte[] benchmark) {
        Exception e = new NotImplementedException("Can't save original benchmark yet");
        throw new BenchFlowMinioClientException(e.getMessage(), e);
    }

    //TODO: implement this?
    public void getOriginalBenchmark(final String benchmarkId) {
        Exception e = new NotImplementedException("Can't retrieve original benchmark yet");
        throw new BenchFlowMinioClientException(e.getMessage(), e);
    }

    /***
     * Removes object at
     * benchmarks/{id}, if it exists
     */
    private void removeIfExists(final String id) {
        try {
            mc.removeObject(BENCHMARKS_BUCKET, id);
        } catch (ErrorResponseException e) {
            /* happens if the object to remove doesn't exist, do nothing */
        } catch (MinioException | XmlPullParserException | NoSuchAlgorithmException |
                InvalidKeyException | IOException e) {
            throw new BenchFlowMinioClientException(e.getMessage(), e);
        }
    }

    /***
     * Retrieves id from minio and returns it as a string
     */
    private String getTextFile(final String id) {
        try {
            InputStream in = mc.getObject(BENCHMARKS_BUCKET, id);
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | XmlPullParserException | IOException e) {
            throw new BenchFlowMinioClientException(e.getMessage(),e);
        }
    }

    /***
     * Saves content at benchmarks/{id}
     */
    private void saveTextFile(final String content, final String id) {
        byte[] toSave = content.getBytes(StandardCharsets.UTF_8);
        try {
            mc.putObject(BENCHMARKS_BUCKET, id, new ByteArrayInputStream(toSave), toSave.length, "application/octet-stream");
        } catch (MinioException | NoSuchAlgorithmException | XmlPullParserException | InvalidKeyException | IOException e) {
            throw new BenchFlowMinioClientException(e.getMessage(), e);
        }
    }

    /***
     * Saves an inputstream at
     * benchmarks/{id}
     */
    private void saveInputStream(final InputStream content, final String id) {
        try {
            byte[] bytes = IOUtils.toByteArray(content);
            InputStream stream = new ByteArrayInputStream(bytes);
            mc.putObject(BENCHMARKS_BUCKET,id,stream,bytes.length,"application/octet-stream");
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException |
                XmlPullParserException | IOException e) {
            throw new BenchFlowMinioClientException(e.getMessage(), e);
        }
    }

    /***
     * Returns file at benchmarks/{id}
     */
    private InputStream getFile(final String id) {
        try {
            return mc.getObject(BENCHMARKS_BUCKET, id);
        } catch (MinioException | NoSuchAlgorithmException | XmlPullParserException |
                InvalidKeyException | IOException e) {
            throw new BenchFlowMinioClientException(e.getMessage(), e);
        }
    }

    /***
     * Returns the deployment descriptor sent at deploy time,
     * saved at benchmarks/{benchmarkId}/original/docker-compose.yml
     */
    public String getOriginalDeploymentDescriptor(final String benchmarkId)  {
        return getTextFile(benchmarkId + "/original/docker-compose.yml");
    }

    /***
     * Returns the benchmark configuration sent at deploy time,
     * saved at benchmarks/{benchmarkId}/original/benchflow-benchmark.yml
     */
    public String getOriginalBenchFlowBenchmark(final String benchmarkId) {
        return getTextFile(benchmarkId + "/original/benchflow-benchmark.yml");
    }

    /***
     * Saves the original deployment descriptor for an experiment,
     * at benchmarks/{benchmarkId}/original/docker-compose.yml
     */
    public void saveOriginalDeploymentDescriptor(final String benchmarkId, final String deploymentDescriptor) {
        saveTextFile(deploymentDescriptor, benchmarkId + "/original/docker-compose.yml");
    }

    /***
     * Saves the original benchmark configuration for an experiment,
     * at benchmarks/{benchmarkId}/original/benchflow-benchmark.yml
     */
    public void saveOriginalBenchFlowBenchmark(final String benchmarkId, final String benchFlowBenchmark) {
        saveTextFile(benchFlowBenchmark, benchmarkId + "/original/benchflow-benchmark.yml");
    }

    /***
     * Returns the deployment descriptor for a trial,
     * from benchmarks/{benchmarkId}/{experimentNumber}/{trialNumber}/docker-compose.yml
     */
    public String getDeploymentDescriptor(final String benchmarkId, final long experimentNumber, final int trialNumber) {
        return getTextFile(benchmarkId + "/" + experimentNumber + "/" + trialNumber + "/docker-compose.yml");
    }

    /***
     * Saves the deployment descriptor generated for a trial at
     * benchmarks/{benchmarkId}/{experimentNumber}/{trialNumber}/docker-compose.yml
     */
    public void saveDeploymentDescriptor(final String benchmarkId, final long experimentNumber,
                                         final int trialNumber, final String descriptor) {
        String id = benchmarkId + "/" + experimentNumber + "/" + trialNumber + "/docker-compose.yml";
        saveTextFile(descriptor, id);
    }

    /***
     * Removes the deployment descriptor for a trial,
     * from benchmarks/{benchmarkId}/{experimentNumber}/{trialNumber}/docker-compose.yml
     */
    public void removeDeploymentDescriptor(final String benchmarkId, final long experimentNumber,
                                           final int trialNumber) {
        removeIfExists(benchmarkId + "/" + experimentNumber + "/" + trialNumber + "/docker-compose.yml");
    }

    /***
     * Saves the benchmark configuration for an experiment at
     * benchmarks/{benchmarkId}/{experimentNumber}/benchflow-benchmark.yml
     */
    public void saveBenchFlowBenchmark(String experimentId) {
        Exception e = new NotImplementedException("Can't save benchflow benchmark yet");
        throw new BenchFlowMinioClientException(e.getMessage(), e);
    }

    /***
     * Returns the configuration for an experiment,
     * from benchmarks/{benchmarkId}/{experimentNumber}/benchflow-benchmark.yml
     */
    public String getBenchFlowBenchmark(final String benchmarkId, final long experimentNumber) {
        return getTextFile(benchmarkId + "/" + experimentNumber + "/benchflow-benchmark.yml");
    }

    /***
     * Removes the configuration for an experiment,
     * from benchmarks/{benchmarkId}/{experimentNumber}/benchflow-benchmark.yml
     */
    public void removeBenchFlowBenchmark(final String benchmarkId, final long experimentNumber) {
        removeIfExists(benchmarkId + "/" + experimentNumber + "/benchflow-benchmark.yml");
    }


    /***
     * Saves the generated Faban configuration for a trial,
     * at benchmarks/{benchmarkId}/{experimentNumber}/{trialNumber}/run.xml
     */
    public void saveFabanConfiguration(final String benchmarkId, final long experimentNumber,
                                       final int trialNumber, final String configuration) {
        String id = benchmarkId + "/" + experimentNumber + "/" + trialNumber + "/run.xml";
        saveTextFile(configuration, id);
    }

    /***
     * Returns the Faban configuration for a trial,
     * from benchmarks/{benchmarkId}/{experimentNumber}/{trialNumber}/run.xml
     */
    public String getFabanConfiguration(final String benchmarkId, final long experimentNumber, final int trialNumber) {
        return getTextFile(benchmarkId + "/" + experimentNumber + "/" + trialNumber + "/run.xml");
    }

    /***
     * Removes the Faban configuration for a trial,
     * from benchmarks/{benchmarkId}/{experimentNumber}/{trialNumber}/run.xml
     */
    public void removeFabanConfiguration(final String benchmarkId, final long experimentNumber, final int trialNumber) {
        removeIfExists(benchmarkId + "/" + experimentNumber + "/" + trialNumber + "/run.xml");
    }

    /***
     * Returns a .zip file containing the sources of a benchmark,
     * from benchmarks/{benchmarkId}/sources.zip
     */
    public InputStream getBenchmarkSources(final String benchmarkId) {
        return getFile(benchmarkId + "/sources.zip");
    }

    /***
     * Saves a .zip file containing the sources of a benchmark,
     * at benchmarks/{benchmarkId}/sources.zip
     */
    public void saveBenchmarkSources(final InputStream sources, final String benchmarkId) {
        saveInputStream(sources, benchmarkId + "/sources.zip");
    }

    /***
     * Returns the driver generated for an experiment,
     * from benchmarks/{benchmarkId}/{experimentNumber}/driver.jar
     */
    public InputStream getGeneratedDriver(final String benchmarkId, final long experimentNumber) {
        return getFile(benchmarkId + "/" + experimentNumber + "/driver.jar");
    }

    /***
     * Saves generated driver for an experiment,
     * at benchmarks/{benchmarkId}/{experimentNumber}/driver.jar
     */
    public void saveGeneratedDriver(final String benchmarkId, final long experimentNUmber, final String driverPath) {
        try {
            mc.putObject(BENCHMARKS_BUCKET, benchmarkId + "/" + experimentNUmber + "/driver.jar", driverPath);
        } catch (MinioException | NoSuchAlgorithmException | InvalidKeyException | IOException | XmlPullParserException e) {
            throw new BenchFlowMinioClientException(e.getMessage(), e);
        }
    }

    /***
     * Saves a model,
     * at benchmarks/{benchmarkId}/models/{modelName}
     */
    public void saveModel(final String benchmarkId, final String modelName, final InputStream model) throws IOException {
        String modelContent = IOUtils.toString(model, "UTF-8");
        saveTextFile(modelContent, benchmarkId + "/models/" + modelName);
    }

    /***
     * Removes benchmarks/{benchmarkId}/original/benchflow-benchmark.yml
     */
    public void removeOriginalBenchFlowBenchmark(final String benchmarkId) {
        removeIfExists(benchmarkId + "/original/benchflow-benchmark.yml");
    }

    /***
     * Removes benchmarks/{benchmarkId}/original/docker-compose.yml
     */
    public void removeOriginalDeploymentDescriptor(final String benchmarkId) {
        removeIfExists(benchmarkId + "/original/docker-compose.yml");
    }

    /***
     * Removes benchmarks/{benchmarkId}/sources.zip
     */
    public void removeSources(final String benchmarkId) {
        removeIfExists(benchmarkId + "/sources.zip");
    }

    /***
     * Removes all models at benchmark/{benchmarkId}/models
     */
    public void removeModels(final String benchmarkId) {
        try {
            for(Result<Item> item : mc.listObjects(BENCHMARKS_BUCKET, benchmarkId + "/models")) {
                mc.removeObject(BENCHMARKS_BUCKET, item.get().objectName());
            }
        } catch (MinioException | XmlPullParserException | NoSuchAlgorithmException |
                InvalidKeyException | IOException e) {
            throw new BenchFlowMinioClientException(e.getMessage(), e);
        }
    }

    /***
     * Returns all models at benchmark/{benchmarkId}/models
     */
    public List<String> listModels(final String benchmarkId) {
        List<String> modelNames = new LinkedList<>();
        try {
            for(Result<Item> item : mc.listObjects(BENCHMARKS_BUCKET, benchmarkId + "/models")) {
                modelNames.add(item.get().objectName());
            }
        } catch (MinioException | XmlPullParserException | NoSuchAlgorithmException |
                InvalidKeyException | IOException e) {
            throw new BenchFlowMinioClientException(e.getMessage(), e);
        }
        return modelNames;
    }

    /***
     * Returns a model for a benchmark,
     * from benchmarks/{benchmarkId}/models/{modelName}
     */
    public String getModel(final String benchmarkId, final String modelName) {
        return getTextFile(benchmarkId + "/models/" + modelName);
    }

}

