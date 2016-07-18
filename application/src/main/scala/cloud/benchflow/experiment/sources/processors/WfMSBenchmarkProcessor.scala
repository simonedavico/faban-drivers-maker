package cloud.benchflow.experiment.sources.processors

import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import cloud.benchflow.test.config.experiment.BenchFlowExperiment
import com.sun.faban.harness.DefaultFabanBenchmark2
import spoon.reflect.declaration.{CtType, ModifierKind, CtClass}
import spoon.reflect.reference.CtTypeReference

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 20/04/16.
  */
class WfMSBenchmarkProcessor(benchFlowBenchmark: BenchFlowExperiment,
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
    //TODO: add statement to deploy models
    println(element.getMethod("preRun").getBody.getStatements.size())
  }

}
