package cloud.benchflow.benchmark.sources.processors.drivers.annotations

import java.util.concurrent.TimeUnit

import cloud.benchflow.benchmark.config.benchflowbenchmark.{Operation, Driver, BenchFlowBenchmark}
import cloud.benchflow.benchmark.heuristics.GenerationDefaults
import cloud.benchflow.benchmark.sources.processors.DriverProcessor
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import com.sun.faban.driver.BenchmarkDriver
import spoon.reflect.code.CtFieldAccess
import spoon.reflect.declaration.CtClass
import spoon.reflect.reference.{CtFieldReference, CtTypeReference}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 05/05/16.
  */
class BenchmarkDriverAnnotationProcessor(bb: BenchFlowBenchmark,
                                         driver: Driver[_ <: Operation],
                                         experimentId: String)(implicit env: DriversMakerEnv)
  extends DriverProcessor(bb, driver, experimentId)(env){

  override def doProcess(e: CtClass[_]): Unit = {

    //adds @BenchmarkDriver annotation
    val benchmarkDriverAnnotation = getFactory.Annotation().annotate(e, classOf[BenchmarkDriver])
    benchmarkDriverAnnotation.addValue("name", driver.getClass.getSimpleName)
//    benchmarkDriverAnnotation.addValue("threadPerScale", 1)
    benchmarkDriverAnnotation.addValue("threadPerScale",
      java.lang.Float.valueOf(env.getHeuristics.scaleBalancer(bb).threadPerScale(driver)))
    benchmarkDriverAnnotation.addValue("opsUnit", "requests")
    benchmarkDriverAnnotation.addValue("metric", "req/s")
    benchmarkDriverAnnotation.addValue("percentiles", GenerationDefaults.percentiles.toArray[String])
    val fieldRead: CtFieldAccess[TimeUnit] = getFactory.Core().createFieldRead()
    val enumReference: CtTypeReference[TimeUnit] = getFactory.Type().createReference(classOf[TimeUnit])
    val fieldReference: CtFieldReference[TimeUnit] = getFactory.Field()
      .createReference(enumReference,enumReference, TimeUnit.NANOSECONDS.name())
    fieldReference.setStatic(true)
    fieldRead.setVariable(fieldReference)
    benchmarkDriverAnnotation.addValue("responseTimeUnit", fieldRead)

  }

}
