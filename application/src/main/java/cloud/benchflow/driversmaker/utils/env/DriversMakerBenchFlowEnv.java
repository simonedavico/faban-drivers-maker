package cloud.benchflow.driversmaker.utils.env;

import java.io.FileNotFoundException;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 03/03/16.
 */
public class DriversMakerBenchFlowEnv extends BenchFlowEnv {

    private String benchFlowServicesPath;
    private String benchFlowComposeAddress;
    private String driverSkeletonPath;

    public DriversMakerBenchFlowEnv(String configPath,
                                    String benchFlowServicesPath,
                                    String benchFlowComposeAddress,
                                    String driverSkeletonPath) throws FileNotFoundException {
        super(configPath);
        this.benchFlowServicesPath = benchFlowServicesPath;
        this.benchFlowComposeAddress = benchFlowComposeAddress;
        this.driverSkeletonPath = driverSkeletonPath;
    }

    public String getBenchFlowServicesPath() {
        return benchFlowServicesPath;
    }

    public String getBenchFlowComposeAddress() {
        return benchFlowComposeAddress;
    }

    public String getDriverSkeletonPath() {
        return driverSkeletonPath;
    }

    public void setDriverSkeletonPath(String driverSkeletonPath) {
        this.driverSkeletonPath = driverSkeletonPath;
    }

}
