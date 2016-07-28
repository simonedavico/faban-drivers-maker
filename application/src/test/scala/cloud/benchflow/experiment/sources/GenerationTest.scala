package cloud.benchflow.experiment.sources

import java.nio.file.Paths

import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.experiment.config.FabanBenchmarkConfigurationBuilder
import cloud.benchflow.experiment.config.deploymentdescriptor.{SiblingVariableResolver, DeploymentDescriptorBuilder}
import cloud.benchflow.experiment.sources.generators.BenchmarkSourcesGenerator
import cloud.benchflow.driversmaker.utils.env.{DriversMakerEnv, ConfigYml}
import cloud.benchflow.test.config.experiment.BenchFlowExperiment
import cloud.benchflow.test.deployment.docker.compose.DockerCompose

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 10/05/16.
  */
object GenerationTest extends App {

  val trial = new Trial
  trial.setBenchmarkId("fooBenchmark")
  trial.setExperimentNumber(1)
  trial.setTrialNumber(1)
  trial.setTotalTrials(3)
  val configYml = new ConfigYml("./application/src/test/resources/app/config.yml")

  val benchFlowEnv = new DriversMakerEnv(configYml,
    "./application/src/test/resources/app/benchflow-services",
    "./application/src/test/resources/app/drivers",
    "8080")

  val dc = scala.io.Source.fromFile("./application/src/test/resources/docker-compose.yml").mkString
  val expConfig = scala.io.Source.fromFile("./application/src/test/resources/benchflow-test.yml").mkString
  val parsedExpConfig = BenchFlowExperiment.fromYaml(expConfig)

  val parsedDc = DockerCompose.fromYaml(dc)

  val dcBuilder = new DeploymentDescriptorBuilder(
    testConfig = parsedExpConfig,
    env = benchFlowEnv
  )
  val resolvedDC = dcBuilder.resolveDeploymentDescriptor(parsedDc, trial)
  println(DockerCompose.toYaml(resolvedDC))

  val runXmlBuilder = new FabanBenchmarkConfigurationBuilder(parsedExpConfig,benchFlowEnv,parsedDc)
//  println(new PrettyPrinter(400, 2).format(runXmlBuilder.build(trial)))

  val siblingResolver = new SiblingVariableResolver(parsedDc, benchFlowEnv, parsedExpConfig)

  val benchmarkSourcesGenerator = BenchmarkSourcesGenerator(
    experimentId = trial.getExperimentId,
    expConfig = parsedExpConfig,
    generatedBenchmarkOutputDir = Paths.get("./application/src/test/resources/generated"),
    env = benchFlowEnv
  )

  benchmarkSourcesGenerator.generate()

}
