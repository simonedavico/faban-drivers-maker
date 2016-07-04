package cloud.benchflow.benchmark.sources.processors

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import spoon.reflect.declaration.CtClass

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 20/04/16.
  */
class HttpBenchmarkProcessor(benchFlowBenchmark: BenchFlowBenchmark, experimentId: String)(implicit env: DriversMakerEnv)
  extends BenchmarkSourcesProcessor(benchFlowBenchmark, experimentId)(env) {

  override protected def doProcess(element: CtClass[_]): Unit = ???
}
