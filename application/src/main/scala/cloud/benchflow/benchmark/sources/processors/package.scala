package cloud.benchflow.benchmark.sources

import cloud.benchflow.benchmark.config.benchflowbenchmark.{WfMSDriver, Operation, Driver, BenchFlowBenchmark}
import spoon.processing.AbstractProcessor
import spoon.reflect.declaration.{CtPackage, CtClass}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 20/04/16.
  */
package object processors {

  /** base class for every processor */
  abstract class BenchmarkSourcesProcessor(val benchflowBenchmark: BenchFlowBenchmark,
                                           val experimentId: String)
    extends AbstractProcessor[CtClass[_]] {

    protected def isProcessable(element: CtClass[_]): Boolean = {
      element.getParent() match {
        //if it's part of benchflow libraries or plugins, don't process it
        case elemPackage: CtPackage =>
             !(elemPackage.getQualifiedName.contains("libraries") ||
               elemPackage.getQualifiedName.contains("plugins"))
        //if it's an inner class, don't process it
        case elemClass: CtClass[_] => false
        case _ => true
      }
    }

    protected def doProcess(element: CtClass[_]): Unit

    final override def process(element: CtClass[_]) = {
      if(isProcessable(element))
        doProcess(element)
    }

  }

  /** a processor specific for a driver */
  abstract class DriverProcessor(benchFlowBenchmark: BenchFlowBenchmark,
                                 driver: Driver[_ <: Operation],
                                 experimentId: String)
    extends BenchmarkSourcesProcessor(benchFlowBenchmark, experimentId)

  /** base class for a processor that generates operations for a driver */
  abstract class DriverOperationsProcessor(benchflowBenchmark: BenchFlowBenchmark,
                                           driver: Driver[_ <: Operation],
                                           experimentId: String)
    extends DriverProcessor(benchflowBenchmark, driver, experimentId)

  /** base class for a processor that generates operations for a wfms driver */
  abstract class WfMSDriverOperationsProcessor(benchFlowBenchmark: BenchFlowBenchmark,
                                               driver: WfMSDriver,
                                               experimentId: String)
    extends DriverOperationsProcessor(benchFlowBenchmark, driver, experimentId)

}
