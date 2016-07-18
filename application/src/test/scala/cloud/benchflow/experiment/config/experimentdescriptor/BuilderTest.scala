package cloud.benchflow.experiment.config.experimentdescriptor

import java.nio.file.Paths

import cloud.benchflow.experiment.sources.generators.BenchmarkSourcesGenerator
import net.jcazevedo.moultingyaml._
import cloud.benchflow.experiment.config._
import cloud.benchflow.experiment.config.experimentdescriptor.BenchFlowExperimentYamlProtocol._
import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.experiment.config.docker.compose.deploymentdescriptor._
import cloud.benchflow.experiment.config.docker.compose.deploymentdescriptor.DeploymentDescriptorBuilder
import cloud.benchflow.experiment.config.docker.compose.deploymentdescriptor.DockerComposeYamlProtocol._
import cloud.benchflow.driversmaker.utils.env.{DriversMakerEnv, BenchFlowEnv}
import cloud.benchflow.experiment.BenchmarkGenerator

import scala.xml.PrettyPrinter

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 13/02/16.
  */
object BuilderTest extends App {

  val deploymentDescriptor =
    """camunda:
      |    image: camundaImage
      |    environment: [ camundavar ]
      |    container_name: mycontainername
      |    ports:
      |        - '8080'
      |service2:
      |    image: s2Image
      |    environment: [ s2var ]
      |    ports:
      |        - '6060'
    """.stripMargin

  val benchFlowBenchmark =
    """sut_name: camunda
      |suts_type: wfms
      |benchmark_name: fooBenchmark
      |description: configuration for testing
      |trials: 5
      |properties:
      |    timeSync: "false"
      |    three:
      |        four: five
      |    six: seven
      |drivers:
      |    - start:
      |        properties:
      |            foo: bar
      |        operations:
      |            - myModel1:
      |                  data: payload
      |sut-configuration:
      |    target-service:
      |        name: camunda
      |        endpoint: /engine-rest
      |    deploy:
      |        camunda: alias1
      |        service2: alias2
      |    benchflow-config:
      |        camunda:
      |            - stats:
      |                 config:
      |                      FOO: resolvedFoo
    """.stripMargin



//  println(benchFlowBenchmark.parseYaml.convertTo[BenchFlowBenchmark])
  //println(BenchFlowBenchmark.fromYaml(benchFlowBenchmark))

  val httpOperation =
    """myOperation:
      |    endpoint: boh
      |    method: get
      |    headers:
      |        Accept: bla
      |    data: payload
    """.stripMargin


//  println(httpOperation.parseYaml.convertTo[HttpOperation])
//  println(httpOperation.parseYaml.convertTo[HttpOperation].toYaml.prettyPrint)
//  println(httpOperation.parseYaml.convertTo[HttpOperation].toYaml.prettyPrint.parseYaml.convertTo[HttpOperation])


  val wfmsOperation =
    """
      |myModel:
      |    data: ciao
    """.stripMargin

//  println(wfmsOperation.parseYaml.convertTo[WfMSOperation])

  val httpDriver =
    """
      |http:
      |    operations:
      |        - myOperation:
      |              method: get
      |              headers:
      |                  Accept: foo
      |              data: payload
      |              endpoint: /operation
    """.stripMargin

//  println(httpDriver.parseYaml.convertTo[HttpDriver])

  val httpDriverWithConfiguration =
    """
      |http:
      |    operations:
      |        - op1:
      |             method: get
      |             headers:
      |                 Accept: foo
      |             data: payload
      |             endpoint: /op1
      |        - op2:
      |             method: put
      |             headers:
      |                 Accept: bar
      |             data: payload
      |             endpoint: /op2
      |    configuration:
      |        mix:
      |            fixedSequence: [ op2, op1 ]
      |        max90th: 2
    """.stripMargin

//  println(httpDriverWithConfiguration.parseYaml.convertTo[HttpDriver])


  val httpDriverWithProperties =
    """
      |http:
      |    properties:
      |        foo: bar
      |    operations:
      |        - myOperation:
      |              method: get
      |              headers:
      |                  Accept: foo
      |              data: payload
      |              endpoint: /operation
    """.stripMargin

//  println(httpDriverWithProperties.parseYaml.convertTo[HttpDriver])

  val wfmsDriver =
    """
      |start:
      |    properties:
      |        foo: bar
      |    operations:
      |        - lala:
      |             data: payload
    """.stripMargin

  val newSutDefinition =
    """
      |name: camunda
      |version: myVersion
    """.stripMargin

 // println(newSutDefinition.parseYaml.convertTo[Sut])

//  println(wfmsDriver.parseYaml.convertTo[WfMSDriver])

  val trial = new Trial
  trial.setBenchmarkId("fooBenchmark")
  trial.setExperimentNumber(1)
  trial.setTrialNumber(1)
  trial.setTotalTrials(3)
  val configYml = new BenchFlowEnv("./application/src/test/resources/app/config.yml")

  val benchFlowEnv = new DriversMakerEnv(configYml,
                                         "./application/src/test/resources/app/benchflow-services",
                                         "./application/src/test/resources/app/drivers",
                                         8080)

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
//  println(resolvedDC.toYaml.prettyPrint)

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
