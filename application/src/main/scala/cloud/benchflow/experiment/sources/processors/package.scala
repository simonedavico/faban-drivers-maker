package cloud.benchflow.experiment.sources

import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import cloud.benchflow.test.config.experiment.{WfMSDriver, Driver, Operation, BenchFlowExperiment}
import spoon.processing.AbstractProcessor
import spoon.reflect.declaration.{CtPackage, CtClass}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 20/04/16.
  */
package object processors {

  /** base class for every processor */
  abstract class BenchmarkSourcesProcessor(val benchflowBenchmark: BenchFlowExperiment,
                                           val experimentId: String)(implicit env: DriversMakerEnv)
    extends AbstractProcessor[CtClass[_]] {

    /***
      * Default implementation prevents processing of:
      * - anonymous classes
      * - inner classes
      * - libraries and plugins
      */
    protected def isProcessable(element: CtClass[_]): Boolean = {
      (element match {
        case elemClass: CtClass[_] => !element.isAnonymous
        case _ => true
      }) &&
      (element.getParent() match {
        //if it's part of benchflow libraries or plugins, don't process it
        case elemPackage: CtPackage =>
             !(elemPackage.getQualifiedName.contains("libraries") ||
               elemPackage.getQualifiedName.contains("plugins"))
        //if it's an inner class, don't process it
        case elemClass: CtClass[_] => false
        case _ => true
      })
    }

    protected def doProcess(element: CtClass[_]): Unit

    final override def process(element: CtClass[_]) = {
      if(isProcessable(element))
        doProcess(element)
    }

  }

  /** a processor specific for a driver */
  abstract class DriverProcessor(benchFlowBenchmark: BenchFlowExperiment,
                                 driver: Driver[_ <: Operation],
                                 experimentId: String)(implicit env: DriversMakerEnv)
    extends BenchmarkSourcesProcessor(benchFlowBenchmark, experimentId)(env)

  /** base class for a processor that generates operations for a driver */
  abstract class DriverOperationsProcessor(benchflowBenchmark: BenchFlowExperiment,
                                           driver: Driver[_ <: Operation],
                                           experimentId: String)(implicit env: DriversMakerEnv)
    extends DriverProcessor(benchflowBenchmark, driver, experimentId)(env)

  /** base class for a processor that generates operations for a wfms driver */
  abstract class WfMSDriverOperationsProcessor(benchFlowBenchmark: BenchFlowExperiment,
                                               driver: WfMSDriver,
                                               experimentId: String)(implicit env: DriversMakerEnv)
    extends DriverOperationsProcessor(benchFlowBenchmark, driver, experimentId)(env)

}
