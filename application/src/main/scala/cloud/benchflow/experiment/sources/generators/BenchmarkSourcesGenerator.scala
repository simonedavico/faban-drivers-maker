package cloud.benchflow.experiment.sources.generators

import java.nio.file.{Paths, Path}

import cloud.benchflow.experiment.sources.processors._
import cloud.benchflow.experiment.sources.processors.drivers.ModelsLoaderProcessor
import cloud.benchflow.experiment.sources.processors.drivers.annotations._
import cloud.benchflow.experiment.sources.processors.drivers.operations.http.HttpDriverOperationsProcessor
import cloud.benchflow.experiment.sources.processors.drivers.operations.wfms.WfMSStartDriverOperationsProcessor
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import cloud.benchflow.test.config.experiment._

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

  def apply(pluginsPath: Path, pluginName: String, version: Version): Path = {

    pluginsPath.resolve(
      allPluginVersions(pluginsPath)
        .find(dirVersion => version.isCompatible(dirVersion)) match {
        case Some(v) => v.toString
        case None => throw new Exception(s"Plugin for version $version couldn't be found.")
      }
    ).resolve(pluginName)

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
                                                                         val benchFlowBenchmark: BenchFlowExperiment,
                                                                         val driver: Driver[_ <: Operation],
                                                                         val experimentId: String)(val env: DriversMakerEnv)
{
  //JVM doesn't allow this, unfortunately
  //val driverOperationsProcessor = new A(benchFlowBenchmark)
  //so we do the same with a reflection workaround:
  private val driverOperationsProcessor =
    scala.reflect.classTag[A].runtimeClass
                             .getConstructor(classOf[BenchFlowExperiment],
                                             driver.getClass,
                                             classOf[String],
                                             classOf[DriversMakerEnv])
                             .newInstance(benchFlowBenchmark, driver, experimentId, env)
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
      spoonLauncher.addProcessor(new TimeAnnotationProcessor(benchFlowBenchmark, experimentId)(env))
      spoonLauncher.addProcessor(new MixAnnotationProcessor(benchFlowBenchmark, driver, experimentId)(env))
      spoonLauncher.addProcessor(new BenchmarkDriverAnnotationProcessor(benchFlowBenchmark, driver, experimentId)(env))

      //apply driver specific processors
      additionalProcessors.foreach(additionalProcessor =>
        spoonLauncher.addProcessor(additionalProcessor)
      )

      spoonLauncher.addInputResource(driverClassTemplate.toString)
      spoonLauncher.run()
  }
}

class HttpDriverGenerator(generatedDriverClassOutputDir: Path,
                          generationResources: Path,
                          benchFlowBenchmark: BenchFlowExperiment,
                          experimentId: String,
                          driver: HttpDriver)(env: DriversMakerEnv)
  extends DriverGenerator[HttpDriverOperationsProcessor](generatedDriverClassOutputDir,
                                                         generationResources,
                                                         benchFlowBenchmark,
                                                         driver,
                                                         experimentId)(env) {

  override def templateResources: Seq[Path] = Seq()
  override def additionalProcessors =
    Seq(new BenchmarkDefinitionAnnotation(benchFlowBenchmark, experimentId)(env))
}

abstract class WfMSDriverGenerator[A <: WfMSDriverOperationsProcessor: ClassTag](generatedDriverClassOutputDir: Path,
                                                                                 generationResources: Path,
                                                                                 benchFlowBenchmark: BenchFlowExperiment,
                                                                                 experimentId: String,
                                                                                 driver: WfMSDriver)(env: DriversMakerEnv)
  extends DriverGenerator[A](generatedDriverClassOutputDir,
                             generationResources,
                             benchFlowBenchmark,
                             driver,
                             experimentId)(env) {

  override def templateResources: Seq[Path] = {
    val pluginsPath = generationResources.resolve(s"plugins/wfms/${benchFlowBenchmark.sut.name}")
    val pluginPath = ResolvePlugin(pluginsPath, "WfMSPlugin.java", benchFlowBenchmark.sut.version)
    val wfmsLibraryPath = generationResources.resolve("libraries/wfms/WfMSApi.java")
    Seq(wfmsLibraryPath, pluginPath)
  }

  override def additionalProcessors: Seq[BenchmarkSourcesProcessor] =
    Seq(new WfMSPluginLoaderProcessor(benchFlowBenchmark, experimentId)(env),
        new ModelsLoaderProcessor(benchFlowBenchmark, experimentId)(env))
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
                               benchFlowBenchmark: BenchFlowExperiment,
                               experimentId: String,
                               driver: WfMSStartDriver)
                              (implicit env: DriversMakerEnv)
  extends WfMSDriverGenerator[WfMSStartDriverOperationsProcessor](generatedDriverClassOutputDir,
                                                                  generationResources,
                                                                  benchFlowBenchmark,
                                                                  experimentId,
                                                                  driver)(env)
{
  override def additionalProcessors: Seq[BenchmarkSourcesProcessor] =
    super.additionalProcessors :+ new BenchmarkDefinitionAnnotation(benchFlowBenchmark, experimentId)(env)
}


/**
  * A generator for a Faban benchmark. Generates the Benchmark and Driver classes
  *
  * @param benchFlowBenchmark configuration from which the benchmark will be generated
  * @param experimentId experiment id
  * @param generatedBenchmarkOutputDir directory where the benchmark will be saved
  * @param env env info (heuristics, resources location, config.yml)
  */
abstract class BenchmarkSourcesGenerator(val benchFlowBenchmark: BenchFlowExperiment,
                                         val experimentId: String,
                                         val generatedBenchmarkOutputDir: Path,
                                         implicit val env: DriversMakerEnv) {

  val generationResources = Paths.get(env.getGenerationResourcesPath)

  protected def benchmarkTemplate: Path
  protected def generateDriversSources(): Unit
  protected def benchmarkGenerationResources: Seq[Path]
  protected def benchmarkGenerationProcessors: Seq[BenchmarkSourcesProcessor]

  val templatesPath = generationResources.resolve("templates")
  val librariesPath = generationResources.resolve("libraries")
  val pluginsPath = generationResources.resolve("plugins")

  protected def generateBenchmarkSource(): Unit = {
    val spoonLauncher = new Launcher

    benchmarkGenerationResources.foreach(resource => {
      spoonLauncher.addTemplateResource(SpoonResourceHelper.createFile(resource.toFile))
    })

    spoonLauncher.setSourceOutputDirectory(generatedBenchmarkOutputDir.resolve("src").toFile)

    benchmarkGenerationProcessors.foreach(processor =>
      spoonLauncher.addProcessor(processor)
    )

    //creates the file benchmark.xml
    spoonLauncher.addProcessor(
      new FabanBenchmarkDeploymentDescriptorProcessor(benchFlowBenchmark,experimentId,generatedBenchmarkOutputDir)(env)
    )

//    val args = Seq("--source-classpath", classPath)
//    spoonLauncher.setArgs(args.toArray)
//    println(spoonLauncher.getEnvironment.getSourceClasspath)

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
            benchFlowBenchmark: BenchFlowExperiment,
            generatedBenchmarkOutputDir: Path,
            env: DriversMakerEnv) =
    benchFlowBenchmark.sut.sutsType match {
      case Http => HttpBenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, env)
      case WfMS => WfMSBenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, env)
    }
}


class HttpBenchmarkSourcesGenerator(benchFlowBenchmark: BenchFlowExperiment,
                                    experimentId: String,
                                    generatedBenchmarkOutputDir: Path,
                                    env: DriversMakerEnv)
  extends BenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, env) {

  val benchmarkTemplate = templatesPath.resolve("harness/http/HttpBenchmark.java")
  override protected def benchmarkGenerationResources: Seq[Path] = Seq()
  override protected def benchmarkGenerationProcessors: Seq[BenchmarkSourcesProcessor] = Seq()

  override protected def generateDriversSources() = {
    val httpDriver = benchFlowBenchmark.drivers.find(_.isInstanceOf[HttpDriver]).get.asInstanceOf[HttpDriver]
    new HttpDriverGenerator(
      generatedBenchmarkOutputDir.resolve("src"),
      generationResources,
      benchFlowBenchmark,
      experimentId,
      httpDriver)(env).generate()
  }
}
object HttpBenchmarkSourcesGenerator {
  def apply(benchFlowBenchmark: BenchFlowExperiment,
            experimentId: String,
            generatedBenchmarkOutputDir: Path,
            env: DriversMakerEnv) =
    new HttpBenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, env)
}


class WfMSBenchmarkSourcesGenerator(benchFlowBenchmark: BenchFlowExperiment,
                                    experimentId: String,
                                    generatedBenchmarkOutputDir: Path,
                                    env: DriversMakerEnv)
  extends BenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, env) {

  val benchmarkTemplate: Path = templatesPath.resolve("harness/wfms/WfMSBenchmark.java")

  override protected def benchmarkGenerationResources: Seq[Path] = {
    //val pluginsPath = generationResources.resolve(s"plugins/wfms/${benchFlowBenchmark.sut.name}")
    val wfmsPluginsPath = pluginsPath.resolve(s"wfms/${benchFlowBenchmark.sut.name}")
    //val pluginsPath = generationResources.resolve(s"plugins/wfms/${benchFlowBenchmark.sut.name}")
    val pluginPath = ResolvePlugin(wfmsPluginsPath, "WfMSPlugin.java", benchFlowBenchmark.sut.version)
    //val wfmsLibraryPath = generationResources.resolve("libraries/wfms/WfMSApi.java")
    val wfmsLibraryPath = librariesPath.resolve("wfms/WfMSApi.java")
    Seq(wfmsLibraryPath, pluginPath)
  }

  override protected def benchmarkGenerationProcessors: Seq[BenchmarkSourcesProcessor] =
    Seq(new WfMSPluginLoaderProcessor(benchFlowBenchmark, experimentId)(env),
        new WfMSBenchmarkProcessor(benchFlowBenchmark, experimentId)(env))

  //for each driver type, create a driver generator and run it
  override protected def generateDriversSources() = {
    val startDriver = benchFlowBenchmark.drivers.find(_.isInstanceOf[WfMSStartDriver]).get.asInstanceOf[WfMSStartDriver]
    new WfMSStartDriverGenerator(
      generatedBenchmarkOutputDir.resolve("src"),
      generationResources,
      benchFlowBenchmark,
      experimentId,
      startDriver)(env).generate()
  }

}
object WfMSBenchmarkSourcesGenerator {
  def apply(benchFlowBenchmark: BenchFlowExperiment,
            experimentId: String,
            generatedBenchmarkOutputDir: Path,
            env: DriversMakerEnv) =
    new WfMSBenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, env)
}