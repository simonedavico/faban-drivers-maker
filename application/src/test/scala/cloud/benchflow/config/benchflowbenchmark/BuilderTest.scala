package cloud.benchflow.config.benchflowbenchmark

import cloud.benchflow.config.BenchFlowBenchmarkConfigurationBuilder
import cloud.benchflow.driversmaker.configurations.FabanDefaults
import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.driversmaker.utils.BenchFlowEnv

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
      |suts_type: WfMS
      |benchmark_name: fooBenchmark
      |description: configuration for testing
      |properties:
      |    one: two
      |    three:
      |        four: five
      |    six: seven
      |drivers:
      |    - driver1:
      |        foo: bar
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

  val trial = new Trial
  trial.setBenchmarkId("fooBenchmark")
  trial.setExperimentNumber(1)
  trial.setTrialNumber(1)
  trial.setTotalTrials(3)
  val benchFlowEnv = new BenchFlowEnv("./application/src/test/resources/app/config.yml",
                               "./application/src/test/resources/app/benchflow-services",
                               "benchFlowComposeAddress",
                               "./application/src/test/resources/app/skeleton")
  val defaults = new FabanDefaults
  defaults.setJavaHome("testJavaHome")
  defaults.setJavaOpts("testJavaOpts")
  val builder = new BenchFlowBenchmarkConfigurationBuilder(benchFlowBenchmark, deploymentDescriptor, benchFlowEnv, defaults)
  println(builder.buildDeploymentDescriptor(trial))
  println(builder.buildFabanBenchmarkConfiguration(trial))
}
