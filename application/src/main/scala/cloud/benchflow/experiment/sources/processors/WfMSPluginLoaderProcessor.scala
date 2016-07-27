package cloud.benchflow.experiment.sources.processors

import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import cloud.benchflow.test.config.experiment.BenchFlowExperiment
import spoon.reflect.declaration.{CtClass, CtType, ModifierKind}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 11/05/16.
  */
class WfMSPluginLoaderProcessor(expConfig: BenchFlowExperiment,
                                experimentId: String)(implicit env: DriversMakerEnv)
  extends BenchmarkSourcesProcessor(expConfig, experimentId)(env) {


  override protected def doProcess(element: CtClass[_]): Unit = {

    val apiType: CtType[_] = getFactory.Type().get("cloud.benchflow.libraries.wfms.WfMSApi")
    val pluginType = getFactory.Type()
      .get(s"cloud.benchflow.plugins.wfms.${expConfig.sut.name}.WfMSPlugin")

    element.addNestedType(apiType)
    element.addNestedType(pluginType)

    //I have to do this to fix the complete name of WfMSPlugin's superclass
    val nestedPluginType: CtType[_] = element.getNestedType("WfMSPlugin")
    val nestedApiType: CtType[_] = element.getNestedType("WfMSApi")

    val pluginField = getFactory.Code().createCtField("plugin", nestedApiType.getReference, "null", ModifierKind.PRIVATE)
    element.addFieldAtTop(pluginField)

    val validateMethodBody = element.getMethod("initialize").getBody
    validateMethodBody.addStatement(
      getFactory.Code().createCodeSnippetStatement("plugin = new WfMSPlugin(sutEndpoint)")
    )

    nestedPluginType.getSuperclass.replace(nestedApiType.getReference)

  }
}
