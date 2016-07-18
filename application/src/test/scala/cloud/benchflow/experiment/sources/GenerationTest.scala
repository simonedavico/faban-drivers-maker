package cloud.benchflow.experiment.sources

import java.nio.file.Paths

import cloud.benchflow.experiment.config.experimentdescriptor.BenchFlowExperiment$
import cloud.benchflow.experiment.sources.generators.BenchmarkSourcesGenerator
import cloud.benchflow.driversmaker.utils.env.{DriversMakerEnv, BenchFlowEnv}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 10/05/16.
  */
object GenerationTest extends App {

  val bb =
    """
      |sut:
      |    name: camunda
      |    version: v1
      |    type: wfms
      |description: A generated benchmark
      |benchmark_name: MyBenchmark
      |trials: 2
      |virtualUsers: 1000
      |execution:
      |    rampUp: 0
      |    steadyState: 60
      |    rampDown: 0
      |properties:
      |    timeSync: "false"
      |drivers:
      |    - start:
      |          operations:
      |              - myModel:
      |                    data: ciaociao
      |          configuration:
      |              deviation: 5
      |sut-configuration:
      |    target-service:
      |        name: camunda
      |        endpoint: /engine-rest
      |    deploy:
      |        camunda: bull
      |    benchflow-config:
      |        camunda: [ stats ]
    """.stripMargin

  val parsedConfiguration = BenchFlowExperiment.fromYaml(bb)

  val configYml = new BenchFlowEnv("./application/src/test/resources/app/config.yml")
//  val benchFlowEnv = new DriversMakerEnv(configYml,
//    "./application/src/test/resources/app/benchflow-services",
//    "./application/src/test/resources/app/drivers/templates/skeleton/benchmark")

  val benchFlowEnv = new DriversMakerEnv(configYml,
    "./application/src/test/resources/app/benchflow-services",
    "./application/src/test/resources/app/drivers", 8080)

  BenchmarkSourcesGenerator(
    experimentId = "MyExperiment.1.1",
    benchFlowBenchmark = parsedConfiguration,
    generatedBenchmarkOutputDir = Paths.get("./application/src/test/resources/generated"),
    env = benchFlowEnv
    //generationResources = Paths.get("./application/src/test/resources/app/drivers")
  ).generate()

}
