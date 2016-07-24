package cloud.benchflow.experiment

import java.nio.file.Path

import cloud.benchflow.experiment.config.FabanBenchmarkConfigurationBuilder
import cloud.benchflow.experiment.config.deploymentdescriptor.DeploymentDescriptorBuilder

import cloud.benchflow.test.config.experiment.BenchFlowExperiment
import cloud.benchflow.test.deployment.docker.compose.DockerCompose

import cloud.benchflow.experiment.sources.generators.BenchmarkSourcesGenerator
import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv

import scala.xml.PrettyPrinter

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 30/05/16.
  */
class BenchmarkGenerator(experimentId: String,
                         experimentDescriptor: String,
                         deploymentDescriptor: String,
                         generatedBenchmarkOutputDir: Path,
                         env: DriversMakerEnv) {

    private val dd = DockerCompose.fromYaml(deploymentDescriptor)
    private val (deploymentDescriptorGenerator: DeploymentDescriptorBuilder,
                 sourcesGenerator: BenchmarkSourcesGenerator,
                 fabanConfigGenerator: FabanBenchmarkConfigurationBuilder) =
    {
      val bb = BenchFlowExperiment.fromYaml(experimentDescriptor)
      (new DeploymentDescriptorBuilder(bb, env),
       BenchmarkSourcesGenerator.apply(experimentId,
                                       bb,
                                       generatedBenchmarkOutputDir,
                                       env),
       new FabanBenchmarkConfigurationBuilder(bb,env,dd))
    }

    def generateSources() = sourcesGenerator.generate()

    def generateFabanConfigurationForTrial(t: Trial): String =
      new PrettyPrinter(400, 2).format(fabanConfigGenerator.build(t))

    def generateDeploymentDescriptorForTrial(t: Trial): String = {
      DockerCompose.toYaml(
        deploymentDescriptorGenerator.resolveDeploymentDescriptor(dd, t)
      )
    }
}