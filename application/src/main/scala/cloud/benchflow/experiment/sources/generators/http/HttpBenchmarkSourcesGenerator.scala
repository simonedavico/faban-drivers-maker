package cloud.benchflow.experiment.sources.generators.http

import java.nio.file.Path

import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv
import cloud.benchflow.experiment.sources.generators.BenchmarkSourcesGenerator
import cloud.benchflow.experiment.sources.processors.BenchmarkSourcesProcessor
import cloud.benchflow.test.config.experiment.BenchFlowExperiment
import cloud.benchflow.test.config.sut.http.HttpDriver

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 25/07/16.
  */
class HttpBenchmarkSourcesGenerator(benchFlowBenchmark: BenchFlowExperiment,
                                    experimentId: String,
                                    generatedBenchmarkOutputDir: Path,
                                    env: DriversMakerEnv)
  extends BenchmarkSourcesGenerator(benchFlowBenchmark, experimentId, generatedBenchmarkOutputDir, env) {

  val benchmarkTemplate = templatesPath.resolve("harness/http/HttpBenchmark.java")
  override protected def benchmarkGenerationResources: Seq[Path] = super.benchmarkGenerationResources
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
