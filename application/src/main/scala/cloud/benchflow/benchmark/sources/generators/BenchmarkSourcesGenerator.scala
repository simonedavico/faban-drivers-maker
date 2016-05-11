package cloud.benchflow.benchmark.sources.generators

import java.nio.file.Path

import cloud.benchflow.benchmark.config.benchflowbenchmark._
import cloud.benchflow.benchmark.sources.processors._
import cloud.benchflow.benchmark.sources.processors.drivers.annotations.{BenchmarkDefinitionAnnotation, BenchmarkDriverAnnotationProcessor, MixAnnotationProcessor, TimeAnnotationProcessor}
import cloud.benchflow.benchmark.sources.processors.drivers.operations.http.HttpDriverOperationsProcessor
import cloud.benchflow.benchmark.sources.processors.drivers.operations.wfms.WfMSStartDriverOperationsProcessor

import spoon.Launcher
import spoon.compiler.SpoonResourceHelper

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/** @author Simone D'Avico (simonedavico@gmail.com) */

/***
  * Utility object to resolve a plugin
  */
object ResolvePlugin {

  private val directoriesFilter = new java.nio.file.DirectoryStream.Filter[Path]() {
    def accept(file: Path) = file.toFile.isDirectory
  }

  private def allPluginVersions(pluginsPath: Path): Seq[Version] = {

    import scala.collection.mutable.ListBuffer

    val dirStream = Try(java.nio.file.Files.newDirectoryStream(pluginsPath, directoriesFilter))
    val pluginVersions = new ListBuffer[Version]()

    dirStream match {

      case Failure(e) => throw e

      case Success(stream) =>
        val iterator = stream.iterator()
        while(iterator.hasNext)
          pluginVersions += Version(iterator.next().getFileName.toString)
        stream.close()
    }

    pluginVersions.toList

  }

  def apply(pluginsPath: Path, version: Version): Path = {

    pluginsPath.resolve(
      allPluginVersions(pluginsPath)
        .find(dirVersion => version.isCompatible(dirVersion)) match {
        case Some(v) => v.toString
        case None => throw new Exception(s"Plugin for version $version couldn't be found.")
      }
    ).resolve("WfMSPlugin.java")

  }

}


/***
  * A generator for a driver class
  *
  * @param generatedDriverClassOutputDir directory where the generated driver will be saved
  * @param generationResources location on file system of generation resources (libraries, plugins, templates)
  * @param benchFlowBenchmark configuration from which the driver will be generated
  * @param driver driver configuration
  * @tparam A implementation of [[DriverOperationsProcessor]]
  */
abstract class DriverGenerator[A <: DriverOperationsProcessor: ClassTag](val generatedDriverClassOutputDir: Path,
                                                                         val generationResources: Path,
                                                                         val benchFlowBenchmark: BenchFlowBenchmark,
                                                                         val driver: Driver[_ <: Operation],
                                                                         val experimentId: String)
{
  //JVM doesn't allow this, unfortunately
  //val driverOperationsProcessor = new A(benchFlowBenchmark)
  //so we do the same with a reflection workaround:
  private val driverOperationsProcessor =
    scala.reflect.classTag[A].runtimeClass
                             .getConstructor(classOf[BenchFlowBenchmark], driver.getClass, classOf[String])
                             .newInstance(benchFlowBenchmark, driver, experimentId)
                             .asInstanceOf[A]

  //each driver generator has to define what template resources has to be added to the spoon launcher
  def templateResources: Seq[Path]
  //each driver generator can specify additional processors
  def additionalProcessors: Seq[BenchmarkSourcesProcessor]

  def generate() = {
      val driverClassTemplate = generationResources.resolve("templates/driver/Driver.java")
      val spoonLauncher = new Launcher
      templateResources.foreach(resource =>
        spoonLauncher.addTemplateResource(SpoonResourceHelper.createFile(resource.toFile))
      )
      spoonLauncher.setSourceOutputDirectory(generatedDriverClassOutputDir.toFile)

      //add processors applied to all drivers
      spoonLauncher.addProcessor(driverOperationsProcessor)
      spoonLauncher.addProcessor(new TimeAnnotationProcessor(benchFlowBenchmark, experimentId))
      spoonLauncher.addProcessor(new MixAnnotationProcessor(benchFlowBenchmark, driver, experimentId))
      spoonLauncher.addProcessor(new BenchmarkDriverAnnotationProcessor(benchFlowBenchmark, driver, experimentId))

      //apply driver specific processors
      additionalProcessors.foreach(additionalProcessor =>
        spoonLauncher.addProcessor(additionalProcessor)
      )

      //spoonLauncher.setArgs(Seq("--level","OFF").toArray)
      spoonLauncher.addInputResource(driverClassTemplate.toString)
      spoonLauncher.run()
  }
}

class HttpDriverGenerator(generatedDriverClassOutputDir: Path,
                          generationResources: Path,
                          benchFlowBenchmark: BenchFlowBenchmark,
                          experimentId: String,
                          driver: HttpDriver)
  extends DriverGenerator[HttpDriverOperationsProcessor](generatedDriverClassOutputDir,
                                                         generationResources,
                                                         benchFlowBenchmark,
                                                         driver,
                                                         experimentId) {

  override def templateResources: Seq[Path] = Seq()
  override def additionalProcessors =
    Seq(new BenchmarkDefinitionAnnotation(benchFlowBenchmark, experimentId))
}

abstract class WfMSDriverGenerator[A <: WfMSDriverOperationsProcessor: ClassTag](generatedDriverClassOutputDir: Path,
                                                                                 generationResources: Path,
                                                                                 benchFlowBenchmark: BenchFlowBenchmark,
                                                                                 experimentId: String,
                                                                                 driver: WfMSDriver)
  extends DriverGenerator[A](generatedDriverClassOutputDir,
                             generationResources,
                             benchFlowBenchmark,
                             driver,
                             experimentId) {


  override def templateResources: Seq[Path] = {
    val pluginsPath = generationResources.resolve(s"plugins/${benchFlowBenchmark.sut.name}")
    val pluginPath = ResolvePlugin(pluginsPath, benchFlowBenchmark.sut.version)
    val wfmsLibraryPath = generationResources.resolve("libraries/WfMSApi.java")
    Seq(wfmsLibraryPath, pluginPath)
  }

  override def additionalProcessors: Seq[BenchmarkSourcesProcessor] =
    Seq(new PluginLoaderProcessor(benchFlowBenchmark, experimentId))

}

/**
  * A generator for a start driver
  *
  * @param generatedDriverClassOutputDir directory where the generated driver will be saved
  * @param generationResources location on file system of generation resources (libraries, plugins, templates)
  * @param benchFlowBenchmark configuration from which the driver will be generated
  * @param driver driver configuration
  */
class WfMSStartDriverGenerator(generatedDriverClassOutputDir: Path,
                               generationResources: Path,
                               benchFlowBenchmark: BenchFlowBenchmark,
                               experimentId: String,
                               driver: WfMSStartDriver)
  extends WfMSDriverGenerator[WfMSStartDriverOperationsProcessor](generatedDriverClassOutputDir,
                                                              generationResources,
                                                              benchFlowBenchmark,
                                                              experimentId,
                                                              driver)
{
  override def additionalProcessors: Seq[BenchmarkSourcesProcessor] =
    super.additionalProcessors :+ new BenchmarkDefinitionAnnotation(benchFlowBenchmark, experimentId)
}


/**
  * A generator for a Faban benchmark. Generates the Benchmark and Driver classes
  *
  * @param benchFlowBenchmark configuration from which the benchmark will be generated
  * @param experimentId experiment id
  * @param generatedBenchmarkOutputDir directory where the benchmark will be saved
  * @param generationResources location on file system of generation resources (libraries, plugins, templates)
  */
abstract class BenchmarkSourcesGenerator(val benchFlowBenchmark: BenchFlowBenchmark,
                                         val experimentId: String,
                                         val generatedBenchmarkOutputDir: Path,
                                         val generationResources: Path) {

  protected def benchmarkTemplate: Path
  protected def generateDriversSources(): Unit
  protected def benchmarkGenerationResources: Seq[Path]
  protected def benchmarkGenerationProcessors: Seq[BenchmarkSourcesProcessor]

  val templatesPath = generationResources.resolve("templates")

  protected def generateBenchmarkSource(): Unit = {
    val spoonLauncher = new Launcher

    benchmarkGenerationResources.foreach(resource => {
      spoonLauncher.addTemplateResource(SpoonResourceHelper.createFile(resource.toFile))
    })

    spoonLauncher.setSourceOutputDirectory(generatedBenchmarkOutputDir.toFile)

    benchmarkGenerationProcessors.foreach(processor =>
      spoonLauncher.addProcessor(processor)
    )

    //creates the file benchmark.xml
    spoonLauncher.addProcessor(
      new FabanBenchmarkDeploymentDescriptorProcessor(benchFlowBenchmark,experimentId,generatedBenchmarkOutputDir)
    )

    spoonLauncher.addInputResource(benchmarkTemplate.toAbsolutePath.toString)
    spoonLauncher.run()
  }

  final def generate() = {
    generateBenchmarkSource()
    generateDriversSources()
  }
}
object BenchmarkSourcesGenerator {
  def apply(experimentId: String,
            benchFlowBenchmark: BenchFlowBenchmark,
            generatedBenchmarkOutputDir: Path,
            generationResources: Path) =
    benchFlowBenchmark.sut.sutsType match {
      case Http => HttpBenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, generationResources)
      case WfMS => WfMSBenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, generationResources)
    }
}


class HttpBenchmarkSourcesGenerator(benchFlowBenchmark: BenchFlowBenchmark,
                                    experimentId: String,
                                    generatedBenchmarkOutputDir: Path,
                                    generationResources: Path)
  extends BenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, generationResources) {

  val benchmarkTemplate = templatesPath.resolve("harness/http/HttpBenchmark.java")
  override protected def benchmarkGenerationResources: Seq[Path] = Seq()
  override protected def benchmarkGenerationProcessors: Seq[BenchmarkSourcesProcessor] = Seq()

  override protected def generateDriversSources() = {
    val httpDriver = benchFlowBenchmark.drivers.find(_.isInstanceOf[HttpDriver]).get.asInstanceOf[HttpDriver]
    new HttpDriverGenerator(
      generatedBenchmarkOutputDir,
      generationResources,
      benchFlowBenchmark,
      experimentId,
      httpDriver).generate()
  }
}
object HttpBenchmarkSourcesGenerator {
  def apply(benchFlowBenchmark: BenchFlowBenchmark,
            experimentId: String,
            generatedBenchmarkOutputDir: Path,
            generationResources: Path) =
    new HttpBenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, generationResources)
}


class WfMSBenchmarkSourcesGenerator(benchFlowBenchmark: BenchFlowBenchmark,
                                    experimentId: String,
                                    generatedBenchmarkOutputDir: Path,
                                    generationResources: Path)
  extends BenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, generationResources) {

  val benchmarkTemplate: Path = templatesPath.resolve("harness/wfms/WfMSBenchmark.java")

  override protected def benchmarkGenerationResources: Seq[Path] = {
    val pluginsPath = generationResources.resolve(s"plugins/${benchFlowBenchmark.sut.name}")
    val pluginPath = ResolvePlugin(pluginsPath, benchFlowBenchmark.sut.version)
    val wfmsLibraryPath = generationResources.resolve("libraries/WfMSApi.java")
    Seq(wfmsLibraryPath, pluginPath)
  }

  override protected def benchmarkGenerationProcessors: Seq[BenchmarkSourcesProcessor] =
    Seq(new PluginLoaderProcessor(benchFlowBenchmark, experimentId))

  //for each driver type, create a driver generator and run it
  override protected def generateDriversSources() = {
    val startDriver = benchFlowBenchmark.drivers.find(_.isInstanceOf[WfMSStartDriver]).get.asInstanceOf[WfMSStartDriver]
    new WfMSStartDriverGenerator(
      generatedBenchmarkOutputDir,
      generationResources,
      benchFlowBenchmark,
      experimentId,
      startDriver).generate()
  }

}
object WfMSBenchmarkSourcesGenerator {
  def apply(benchFlowBenchmark: BenchFlowBenchmark,
            experimentId: String,
            generatedBenchmarkOutputDir: Path,
            generationResources: Path) =
    new WfMSBenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, generationResources)
}








