package cloud.benchflow.benchmark.sources.processors.drivers.operations.wfms

import cloud.benchflow.benchmark.config.benchflowbenchmark.{Operation, WfMSStartDriver, BenchFlowBenchmark}
import cloud.benchflow.benchmark.sources.processors._
import com.sun.faban.driver.{Timing, BenchmarkOperation}
import spoon.reflect.code.{CtIf, CtFieldAccess, CtCodeSnippetExpression}
import spoon.reflect.declaration.{ModifierKind, CtMethod, CtClass}
import spoon.reflect.reference.{CtFieldReference, CtTypeReference}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * An implementation of [[WfMSDriverOperationsProcessor]] that generates
  * operations and related annotations for a wfms benchmark
  */
class WfMSStartDriverOperationsProcessor(benchFlowBenchmark: BenchFlowBenchmark, driver: WfMSStartDriver, experimentId: String)
  extends WfMSDriverOperationsProcessor(benchFlowBenchmark, driver, experimentId)  {

  override def doProcess(e: CtClass[_]): Unit = {

    e.setSimpleName(driver.getClass.getSimpleName)

    def generateOperation(op: Operation): Unit = {

        val methodName = s"do${op.name}Request"
        val methodBody = getFactory.Core().createBlock()
        val method: CtMethod[Void] = getFactory.Method()
          .create(e,
            getFactory.Code().modifiers(ModifierKind.PUBLIC),
            getFactory.Type().VOID_PRIMITIVE,
            methodName,
            null, null, methodBody)

      val isStartedCheck: CtCodeSnippetExpression[java.lang.Boolean] = getFactory.Code().createCodeSnippetExpression("isStarted()")

      val pluginCall = getFactory.Code().createCodeSnippetStatement(s"""wfms.startProcessInstance("${op.name}")""")
      val mockCall = getFactory.Code().createCodeSnippetStatement("""wfms.startProcessInstance("mock.bpmn")""")

      val ifStatement: CtIf = getFactory.Core().createIf()
      methodBody.addStatement(ifStatement
        .setCondition[CtIf](isStartedCheck)
        .setThenStatement[CtIf](pluginCall)
        .setElseStatement[CtIf](mockCall))

      //add @BenchmarkOperation annotation
      val benchmarkOperationAnnotation = getFactory.Annotation().annotate(method, classOf[BenchmarkOperation])
      val benchmarkOperationName = getFactory.Code().createLiteral(op.name)
      benchmarkOperationAnnotation.addValue("name", benchmarkOperationName)
      benchmarkOperationAnnotation.addValue("max90th", 20000)
      val fieldRead: CtFieldAccess[Timing] = getFactory.Core().createFieldRead()
      val enumReference: CtTypeReference[Timing] = getFactory.Type().createReference(classOf[Timing])
      val fieldReference: CtFieldReference[Timing] = getFactory.Field()
          .createReference(enumReference, enumReference, Timing.AUTO.name())
      fieldReference.setStatic(true)
      fieldRead.setVariable(fieldReference)
      benchmarkOperationAnnotation.addValue("timing", fieldRead)

    }

    driver.operations.foreach(generateOperation)

  }


}
