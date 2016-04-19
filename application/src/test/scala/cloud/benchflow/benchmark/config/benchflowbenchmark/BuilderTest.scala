package cloud.benchflow.benchmark.config.benchflowbenchmark

import net.jcazevedo.moultingyaml._
import cloud.benchflow.benchmark.config._
import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmarkYamlProtocol._
import cloud.benchflow.benchmark.config.BenchFlowBenchmarkConfigurationBuilder
import cloud.benchflow.driversmaker.configurations.FabanDefaults
import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.driversmaker.utils.env.{DriversMakerBenchFlowEnv, BenchFlowEnv}

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
      |suts_type: http
      |benchmark_name: fooBenchmark
      |description: configuration for testing
      |trials: 5
      |properties:
      |    timeSync: "false"
      |    three:
      |        four: five
      |    six: seven
      |drivers:
      |    - driver1:
      |        properties:
      |            foo: bar
      |        operations:
      |            - myOperation:
      |                  method: get
      |                  data: payload
      |                  endpoint: /operation
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
  println(BenchFlowBenchmark.fromYaml(benchFlowBenchmark))

  val httpOperation =
    """myOperation:
      |    method: get
      |    headers:
      |        Accept: stocazzo
      |    data: payload
    """.stripMargin

//  println(httpOperation.parseYaml.convertTo[HttpOperation])

  val wfmsOperation =
    """
      |myModel:
      |    data: ciao
    """.stripMargin

//  println(wfmsOperation.parseYaml.convertTo[WfMSOperation])

  val httpDriver =
    """
      |httpDriverName:
      |    operations:
      |        - myOperation:
      |              method: get
      |              headers:
      |                  Accept: stocazzo
      |              data: payload
      |              endpoint: /operation
    """.stripMargin

//  println(httpDriver.parseYaml.convertTo[HttpDriver])

  val httpDriverWithProperties =
    """
      |http:
      |    properties:
      |        foo: bar
      |    operations:
      |        - myOperation:
      |              method: get
      |              headers:
      |                  Accept: stocazzo
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

//  println(wfmsDriver.parseYaml.convertTo[WfMSDriver])

  val trial = new Trial
  trial.setBenchmarkId("fooBenchmark")
  trial.setExperimentNumber(1)
  trial.setTrialNumber(1)
  trial.setTotalTrials(3)
  val benchFlowEnv = new DriversMakerBenchFlowEnv("./application/src/test/resources/app/config.yml",
                                                  "./application/src/test/resources/app/benchflow-services",
                                                  "benchFlowComposeAddress",
                                                  "./application/src/test/resources/app/skeleton")
  val defaults = new FabanDefaults
  defaults.setJavaHome("testJavaHome")
  defaults.setJavaOpts("testJavaOpts")

  val dc = scala.io.Source.fromFile("./application/src/test/resources/docker-compose.yml").mkString
  val bb = scala.io.Source.fromFile("./application/src/test/resources/benchflow-benchmark.yml").mkString
  val builder = new BenchFlowBenchmarkConfigurationBuilder(bb, dc, benchFlowEnv, defaults)
////  val builder = new BenchFlowBenchmarkConfigurationBuilder(benchFlowBenchmark, deploymentDescriptor, benchFlowEnv, defaults)
//  println(builder.buildDeploymentDescriptor(trial))
//  println(builder.buildFabanBenchmarkConfiguration(trial))
}
