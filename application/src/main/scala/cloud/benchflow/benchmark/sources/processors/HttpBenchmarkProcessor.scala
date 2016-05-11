package cloud.benchflow.benchmark.sources.processors

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import spoon.reflect.declaration.CtClass

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 20/04/16.
  */
class HttpBenchmarkProcessor(benchFlowBenchmark: BenchFlowBenchmark, experimentId: String)
  extends BenchmarkSourcesProcessor(benchFlowBenchmark, experimentId) {

  override protected def doProcess(element: CtClass[_]): Unit = ???
}
