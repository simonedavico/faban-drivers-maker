package cloud.benchflow.driversmaker.utils.minio;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 26/02/16.
 */
public class BenchFlowMinioClientException extends RuntimeException {

    public BenchFlowMinioClientException(String msg, Throwable e) {
        super(msg, e);
    }

}
