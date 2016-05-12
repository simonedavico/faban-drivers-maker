package cloud.benchflow.benchmark.sources.processors

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import spoon.reflect.declaration.{CtClass, CtType, ModifierKind}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 11/05/16.
  */
class PluginLoaderProcessor(benchFlowBenchmark: BenchFlowBenchmark,
                            experimentId: String) extends BenchmarkSourcesProcessor(benchFlowBenchmark, experimentId) {


  override protected def doProcess(element: CtClass[_]): Unit = {

    val apiType: CtType[_] = getFactory.Type().get("cloud.benchflow.libraries.WfMSApi")
    val pluginType = getFactory.Type()
      .get(s"cloud.benchflow.plugins.${benchFlowBenchmark.sut.name}.${benchFlowBenchmark.sut.version}.WfMSPlugin")

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
