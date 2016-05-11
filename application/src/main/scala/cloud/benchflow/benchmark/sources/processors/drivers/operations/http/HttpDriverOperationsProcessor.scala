package cloud.benchflow.benchmark.sources.processors.drivers.operations.http

import cloud.benchflow.benchmark.config.benchflowbenchmark.{HttpDriver, BenchFlowBenchmark}
import cloud.benchflow.benchmark.sources.processors._
import spoon.reflect.declaration.CtClass

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * An implementation of [[DriverOperationsProcessor]] that generates
  * operations and related annotations for an http driver
  */
class HttpDriverOperationsProcessor(benchFlowBenchmark: BenchFlowBenchmark, driver: HttpDriver, experimentId: String)
  extends DriverOperationsProcessor(benchFlowBenchmark, driver, experimentId) {

  override def doProcess(e: CtClass[_]): Unit = {
    //TODO: add benchmarkdefinition annotation
    e.setSimpleName(driver.getClass.getSimpleName)
  }
}
