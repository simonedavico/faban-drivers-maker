package cloud.benchflow.benchmark.sources.processors.drivers.operations.http

import cloud.benchflow.benchmark.config.benchflowbenchmark.{HttpDriver, BenchFlowBenchmark}
import cloud.benchflow.benchmark.sources.processors._
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import spoon.reflect.declaration.CtClass

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * An implementation of [[DriverOperationsProcessor]] that generates
  * operations and related annotations for an http driver
  */
class HttpDriverOperationsProcessor(benchFlowBenchmark: BenchFlowBenchmark,
                                    driver: HttpDriver,
                                    experimentId: String)(implicit env: DriversMakerEnv)
  extends DriverOperationsProcessor(benchFlowBenchmark, driver, experimentId)(env) {

  override def doProcess(e: CtClass[_]): Unit = {
    e.setSimpleName(driver.getClass.getSimpleName)

    //TODO: generate operations
  }
}
