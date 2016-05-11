package cloud.benchflow.benchmark.harness;

import com.sun.faban.harness.Validate;
import com.sun.faban.harness.DefaultFabanBenchmark2;

public class WfMSBenchmark extends DefaultFabanBenchmark2 {

    @Override
    @Validate
    public void validate() throws Exception {
        initialize();
    }

    private void initialize() {

    }

}