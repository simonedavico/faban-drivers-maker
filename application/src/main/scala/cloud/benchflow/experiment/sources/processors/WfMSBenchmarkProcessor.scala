package cloud.benchflow.experiment.sources.processors

import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import cloud.benchflow.test.config.experiment.BenchFlowExperiment
import com.sun.faban.harness.DefaultFabanBenchmark2
import spoon.reflect.code._
import spoon.reflect.declaration.CtClass
import spoon.reflect.reference.CtTypeReference
import spoon.reflect.visitor.Filter

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

  //add plugin.deploy statement to preRun method in Benchmark class
  //CAREFUL!!! any changes to WfMSBenchmark.preRun method
  //could impact on this processor!
  override def doProcess(element: CtClass[_]): Unit = {

    val ifBody = element.getMethod("preRun").getBody.getStatement[CtFor](10)
                               .getBody.asInstanceOf[CtBlock[_]].getStatement(0)
                               .asInstanceOf[CtIf]
                               .getThenStatement[CtBlock[_]]

    ifBody.insertBefore(
      new Filter[CtUnaryOperator[_]] {
        override def matches(t: CtUnaryOperator[_]): Boolean = {
          true
        }
      },
      getFactory.Code().createCodeSnippetStatement(
        "processDefinitionId = plugin.deploy(modelFile).get(modelName)"
      )
    )
  }

}
