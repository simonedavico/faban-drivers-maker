package cloud.benchflow.benchmark.config

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.benchmark.config.docker.compose.DockerCompose
import cloud.benchflow.driversmaker.configurations.FabanDefaults
import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.driversmaker.utils.env.{DriversMakerBenchFlowEnv, BenchFlowEnv}
import scala.xml.PrettyPrinter

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 25/02/16.
  */
class BenchFlowBenchmarkConfigurationBuilder(benchFlowBenchmark: String,
                                             deploymentDescriptor: String,
                                             val benchFlowEnv: DriversMakerBenchFlowEnv,
                                             val fabanDefaults: FabanDefaults) {

  val bb = BenchFlowBenchmark.fromYaml(benchFlowBenchmark)
  val dd = DockerCompose.fromYaml(deploymentDescriptor)

  def buildDeploymentDescriptor(trial: Trial) =
      new DeploymentDescriptorBuilder(bb, benchFlowEnv)
                               .build(dd, trial).toString


  def buildFabanBenchmarkConfiguration(trial: Trial) = {
    val config = new FabanBenchmarkConfigurationBuilder(bb, benchFlowEnv, fabanDefaults, dd).build(trial)
    new PrettyPrinter(400, 2).format(config)
  }

}
