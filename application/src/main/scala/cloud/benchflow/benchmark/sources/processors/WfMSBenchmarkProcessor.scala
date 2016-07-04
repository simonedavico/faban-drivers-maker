package cloud.benchflow.benchmark.sources.processors

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import com.sun.faban.harness.DefaultFabanBenchmark2
import spoon.reflect.declaration.{CtType, ModifierKind, CtClass}
import spoon.reflect.reference.CtTypeReference

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 20/04/16.
  */
class WfMSBenchmarkProcessor(benchFlowBenchmark: BenchFlowBenchmark,
                             experimentId: String)(implicit env: DriversMakerEnv)
  extends BenchmarkSourcesProcessor(benchFlowBenchmark, experimentId)(env) {

  override def isProcessable(element: CtClass[_]): Boolean = {
    super.isProcessable(element) &&
    (element.getSuperclass match {
      //is processable only if it extends DefaultFabanBenchmark2
      case aClass: CtTypeReference[_] => aClass.getActualClass == classOf[DefaultFabanBenchmark2]
      case _ => false
    })
  }

  override def doProcess(element: CtClass[_]): Unit = {

  }

}
