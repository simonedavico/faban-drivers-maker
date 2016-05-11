package cloud.benchflow.benchmark.sources.processors.drivers.annotations

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.benchmark.sources.processors.BenchmarkSourcesProcessor
import com.sun.faban.driver.{NegativeExponential, CycleType, FixedTime}
import spoon.reflect.code.CtFieldAccess
import spoon.reflect.declaration.CtClass
import spoon.reflect.reference.{CtFieldReference, CtTypeReference}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 01/05/16.
  */
class TimeAnnotationProcessor(benchFlowBenchmark: BenchFlowBenchmark,
                              experimentId: String)
  extends BenchmarkSourcesProcessor(benchFlowBenchmark, experimentId) {

  override def doProcess(e: CtClass[_]): Unit = {

    //TODO: implement all other time types (NegativeExponential, ...)
    val timeAnnotation = getFactory.Annotation().annotate(e, classOf[FixedTime])
    timeAnnotation.addValue("cycleTime", 1000)
    timeAnnotation.addValue("cycleDeviation", 5)

    val fieldRead: CtFieldAccess[CycleType] = getFactory.Core().createFieldRead()
    val enumReference: CtTypeReference[CycleType] = getFactory.Type().createReference(classOf[CycleType])
    val fieldReference: CtFieldReference[CycleType] = getFactory.Field()
      .createReference(enumReference, enumReference, CycleType.THINKTIME.name())
    fieldReference.setStatic(true)
    fieldRead.setVariable(fieldReference)
    timeAnnotation.addValue("cycleType", fieldRead)

  }
}
