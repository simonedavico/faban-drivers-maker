package cloud.benchflow.benchmark.sources.processors.drivers.annotations

import cloud.benchflow.benchmark.config.benchflowbenchmark._
import cloud.benchflow.benchmark.sources.processors.{DriverProcessor, BenchmarkSourcesProcessor}
import com.sun.faban.driver.{OperationSequence, Row}
import spoon.reflect.code.CtNewArray
import spoon.reflect.declaration.{CtAnnotation, CtClass}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 01/05/16.
  */
class MixAnnotationProcessor(benchFlowBenchmark: BenchFlowBenchmark,
                             driver: Driver[_ <: Operation],
                             experimentId: String)
  extends DriverProcessor(benchFlowBenchmark, driver, experimentId){


  private def createMatrixMix(e: CtClass[_], mix: MatrixMix): Unit = {

    val matrixMixAnnotation = getFactory.Annotation().annotate(e, classOf[com.sun.faban.driver.MatrixMix])

    //add operations
    matrixMixAnnotation.addValue("operations", driver.operations.map(_.name).toArray[String])

    //add rows
    val rowAnnotationsArray: CtNewArray[CtAnnotation[Row]] = getFactory.Core().createNewArray[CtAnnotation[Row]]()

    mix.rows.foreach(row => {
      val rowAnnotation: CtAnnotation[Row] = getFactory.Core().createAnnotation()
      rowAnnotation.setAnnotationType(getFactory.Type().createReference(classOf[Row]))
      val rowArray = row.row.map(java.lang.Double.valueOf).toArray[java.lang.Double]
      rowAnnotation.addValue("value", rowArray)
      rowAnnotationsArray.addElement[CtNewArray[CtAnnotation[Row]]](rowAnnotation)
    })

    matrixMixAnnotation.addValue("mix", rowAnnotationsArray)

    //add deviation
    matrixMixAnnotation.addValue(
      "deviation",
      java.lang.Double.valueOf(
        mix.deviation.getOrElse(MixAnnotationProcessor.DEFAULT_DEVIATION)
      )
    )
  }

  private def createFixedSequenceMix(e: CtClass[_], mix: FixedSequenceMix): Unit = {

    val fixedSequenceMixAnnotation = getFactory.Annotation().annotate(e, classOf[com.sun.faban.driver.FixedSequence])

    //adds sequence of operations
    fixedSequenceMixAnnotation.addValue("value", mix.sequence.toArray[String])

    //add deviation
    fixedSequenceMixAnnotation.addValue(
      "deviation",
      java.lang.Double.valueOf(
        mix.deviation.getOrElse(MixAnnotationProcessor.DEFAULT_DEVIATION)
      )
    )

  }

  private def createFlatMix(e: CtClass[_], mix: FlatMix): Unit = {

    val flatMixAnnotation = getFactory.Annotation().annotate(e, classOf[com.sun.faban.driver.FlatMix])

    //add mix
    val opsMix = mix.opsMix.map(java.lang.Double.valueOf).toArray[java.lang.Double]

    flatMixAnnotation.addValue("mix", opsMix)

    //add deviation
    flatMixAnnotation.addValue(
      "deviation",
      java.lang.Double.valueOf(
        mix.deviation.getOrElse(MixAnnotationProcessor.DEFAULT_DEVIATION)
      )
    )

  }

  private def createFlatSequenceMix(e: CtClass[_], mix: FlatSequenceMix): Unit = {

    val flatSequenceMixAnnotation = getFactory.Annotation().annotate(e, classOf[com.sun.faban.driver.FlatSequenceMix])

    //add mix
    flatSequenceMixAnnotation.addValue(
      "mix",
      mix.opsMix.map(java.lang.Double.valueOf).toArray[java.lang.Double]
    )

    //add deviation
    flatSequenceMixAnnotation.addValue(
      "deviation",
      java.lang.Double.valueOf(
        mix.deviation.getOrElse(MixAnnotationProcessor.DEFAULT_DEVIATION)
      )
    )

    //add sequences
    val operationSequenceAnnotationsArray: CtNewArray[CtAnnotation[OperationSequence]] =
      getFactory.Core().createNewArray[CtAnnotation[OperationSequence]]()

    mix.rows.foreach(operationSequence => {
      val operationSequenceAnnotation = getFactory.Core().createAnnotation[OperationSequence]()
      operationSequenceAnnotation.setAnnotationType(getFactory.Type().createReference(classOf[OperationSequence]))
      operationSequenceAnnotation.addValue("value", operationSequence.row.toArray[String])
      operationSequenceAnnotationsArray.addElement[CtNewArray[CtAnnotation[OperationSequence]]](operationSequenceAnnotation)
    })

    flatSequenceMixAnnotation
      .addValue[CtAnnotation[com.sun.faban.driver.FlatSequenceMix]]("sequences", operationSequenceAnnotationsArray)

  }

  override def doProcess(e: CtClass[_]): Unit = {
    //if there is configuration, I use it
    //otherwise I don't add the annotation since FlatMix should already be the default for Faban
    //see http://faban.org/1.3/docs/api/com/sun/faban/driver/FlatMix.html
    driver.configuration.map((maybeConfiguration: DriverConfiguration) => {
      maybeConfiguration.mix.map((mix: Mix) => mix match {
        case matrix: MatrixMix => createMatrixMix(e, matrix)
        case fixedSequence: FixedSequenceMix => createFixedSequenceMix(e, fixedSequence)
        case flat: FlatMix => createFlatMix(e, flat)
        case flatSequence: FlatSequenceMix => createFlatSequenceMix(e, flatSequence)
      })
    })

  }

}
object MixAnnotationProcessor {
  val DEFAULT_DEVIATION = 5
}
