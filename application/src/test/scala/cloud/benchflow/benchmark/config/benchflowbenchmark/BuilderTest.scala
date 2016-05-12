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
      |    method: get
      |    headers:
      |        Accept: bla
      |    data: payload
    """.stripMargin

//  println(httpOperation.parseYaml.convertTo[HttpOperation])

  val wfmsOperation =
    """
      |myModel:
      |    data: ciao
    """.stripMargin

//  println(wfmsOperation.parseYaml.convertTo[WfMSOperation])

  val matrixDriverConfiguration =
    """
      |mix:
      |    matrix:
      |        - [ 1, 2, 3 ]
      |        - [ 4, 5, 6 ]
      |    deviation: 5
      |max90th: 2
    """.stripMargin

  //println(matrixDriverConfiguration.parseYaml.convertTo[DriverConfiguration])

  val flatDriverConfiguration =
    """
      |mix:
      |    flat: [ 33, 33, 33 ]
      |    deviation: 2
    """.stripMargin

  //println(flatDriverConfiguration.parseYaml.convertTo[DriverConfiguration])

  val flatSequenceDriverConfiguration =
    """
      |mix:
      |    flatSequence:
      |        sequences:
      |            - [ myOp1, myOp2 ]
      |            - [ myOp2, myOp1 ]
      |        flat: [ 50, 50 ]
      |    deviation: 5
    """.stripMargin

//  println(flatSequenceDriverConfiguration.parseYaml.convertTo[DriverConfiguration])

  val sequenceDriverConfiguration =
    """
      |mix:
      |    fixedSequence: [ op1, op2, op3 ]
    """.stripMargin

//  println(sequenceDriverConfiguration.parseYaml.convertTo[DriverConfiguration])

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
  val benchFlowEnv = new DriversMakerBenchFlowEnv("./application/src/test/resources/app/config.yml",
                                                  "./application/src/test/resources/app/benchflow-services",
                                                  "./application/src/test/resources/app/drivers/templates/skeleton/benchmark")
  val defaults = new FabanDefaults
  defaults.setJavaHome("testJavaHome")
  defaults.setJavaOpts("testJavaOpts")

  val dc = scala.io.Source.fromFile("./application/src/test/resources/docker-compose.yml").mkString
  val bb = scala.io.Source.fromFile("./application/src/test/resources/benchflow-benchmark.yml").mkString
  val builder = new BenchFlowBenchmarkConfigurationBuilder(bb, dc, benchFlowEnv, defaults)
//  println(builder.bb)
////  val builder = new BenchFlowBenchmarkConfigurationBuilder(benchFlowBenchmark, deploymentDescriptor, benchFlowEnv, defaults)
//  println(builder.buildDeploymentDescriptor(trial))
  println(builder.buildFabanBenchmarkConfiguration(trial))
}
