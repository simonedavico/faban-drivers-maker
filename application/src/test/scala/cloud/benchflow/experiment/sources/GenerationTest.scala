package cloud.benchflow.experiment.sources

import java.nio.file.Paths

import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.experiment.config.FabanBenchmarkConfigurationBuilder
import cloud.benchflow.experiment.config.deploymentdescriptor.{SiblingVariableResolver, DeploymentDescriptorBuilder}
import cloud.benchflow.experiment.sources.generators.BenchmarkSourcesGenerator
import cloud.benchflow.driversmaker.utils.env.{DriversMakerEnv, BenchFlowEnv}
import cloud.benchflow.test.config.experiment.BenchFlowExperiment
import cloud.benchflow.test.deployment.docker.compose.DockerCompose

import net.jcazevedo.moultingyaml._

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 10/05/16.
  */
object GenerationTest extends App {

  import cloud.benchflow.test.deployment.docker.compose.DockerComposeYamlProtocol._

  val trial = new Trial
  trial.setBenchmarkId("fooBenchmark")
  trial.setExperimentNumber(1)
  trial.setTrialNumber(1)
  trial.setTotalTrials(3)
  val configYml = new BenchFlowEnv("./application/src/test/resources/app/config.yml")

  val benchFlowEnv = new DriversMakerEnv(configYml,
    "./application/src/test/resources/app/benchflow-services",
    "./application/src/test/resources/app/drivers",
    "8080")

  val dc = scala.io.Source.fromFile("./application/src/test/resources/docker-compose.yml").mkString
  val bb = scala.io.Source.fromFile("./application/src/test/resources/benchflow-benchmark.yml").mkString
  val parsedBB = BenchFlowExperiment.fromYaml(bb)

  val parsedDc = DockerCompose.fromYaml(dc)
  val parsedTestConfig = BenchFlowExperiment.fromYaml(bb)

  val dcBuilder = new DeploymentDescriptorBuilder(
    testConfig = parsedTestConfig,
    env = benchFlowEnv
  )
  val resolvedDC = dcBuilder.resolveDeploymentDescriptor(parsedDc, trial)
  //  println(resolvedDC)
  //println(resolvedDC.toYaml.prettyPrint)
  println(DockerCompose.toYaml(resolvedDC))

  val runXmlBuilder = new FabanBenchmarkConfigurationBuilder(parsedBB,benchFlowEnv,parsedDc)
  //println(new PrettyPrinter(400, 2).format(runXmlBuilder.build(trial)))

  val siblingResolver = new SiblingVariableResolver(parsedDc, benchFlowEnv, parsedBB)

  val benchmarkSourcesGenerator = BenchmarkSourcesGenerator(
    experimentId = trial.getExperimentId,
    benchFlowBenchmark = parsedBB,
    generatedBenchmarkOutputDir = Paths.get("./application/src/test/resources/generated"),
    env = benchFlowEnv
  )

  benchmarkSourcesGenerator.generate()

}
