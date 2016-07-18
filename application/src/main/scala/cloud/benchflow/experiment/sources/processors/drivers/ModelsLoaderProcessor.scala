package cloud.benchflow.experiment.sources.processors.drivers

import cloud.benchflow.experiment.config.experimentdescriptor.BenchFlowExperiment
import cloud.benchflow.experiment.sources.processors.BenchmarkSourcesProcessor
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import spoon.reflect.declaration.CtClass

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 12/06/16.
  */
class ModelsLoaderProcessor(benchFlowBenchmark: BenchFlowExperiment,
                            experimentId: String)(implicit env: DriversMakerEnv)
  extends BenchmarkSourcesProcessor(benchFlowBenchmark, experimentId)(env) {

  override protected def doProcess(element: CtClass[_]): Unit = {

    //adds call to loadModelsInfo in Driver's constructor
    val loadModelsStmt = getFactory.Code().createCodeSnippetStatement("loadModelsInfo()")
    element.getConstructor().getBody.addStatement(loadModelsStmt)

  }

}