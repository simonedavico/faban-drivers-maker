package cloud.benchflow.experiment.sources.generators.wfms

import java.nio.file.Path

import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import cloud.benchflow.experiment.sources.generators.DriverGenerator
import cloud.benchflow.experiment.sources.processors.{WfMSPluginLoaderProcessor, BenchmarkSourcesProcessor, WfMSDriverOperationsProcessor}
import cloud.benchflow.experiment.sources.utils.ResolvePlugin
import cloud.benchflow.test.config.experiment.BenchFlowExperiment
import cloud.benchflow.test.config.sut.wfms.WfMSDriver

import scala.reflect.ClassTag

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 25/07/16.
  */
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
    Seq(new WfMSPluginLoaderProcessor(benchFlowBenchmark, experimentId)(env))
  //, new ModelsLoaderProcessor(benchFlowBenchmark, experimentId)(env))
}
