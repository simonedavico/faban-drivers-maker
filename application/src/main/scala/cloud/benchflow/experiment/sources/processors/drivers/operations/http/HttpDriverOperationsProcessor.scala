package cloud.benchflow.experiment.sources.processors.drivers.operations.http

import cloud.benchflow.experiment.sources.processors._
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import cloud.benchflow.test.config.experiment.{HttpDriver, BenchFlowExperiment}
import spoon.reflect.declaration.CtClass

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * An implementation of [[DriverOperationsProcessor]] that generates
  * operations and related annotations for an http driver
  */
class HttpDriverOperationsProcessor(benchFlowBenchmark: BenchFlowExperiment,
                                    driver: HttpDriver,
                                    experimentId: String)(implicit env: DriversMakerEnv)
  extends DriverOperationsProcessor(benchFlowBenchmark, driver, experimentId)(env) {

  override def doProcess(e: CtClass[_]): Unit = {
    e.setSimpleName(driver.getClass.getSimpleName)

    //TODO: generate operations
  }
}
