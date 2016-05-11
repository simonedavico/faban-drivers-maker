package cloud.benchflow.benchmark.sources

import java.nio.file.Paths

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.benchmark.sources.generators.BenchmarkSourcesGenerator

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
      |virtualUsers: 50
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

  val parsedConfiguration = BenchFlowBenchmark.fromYaml(bb)

  BenchmarkSourcesGenerator(
    experimentId = "MyExperiment.1.1",
    benchFlowBenchmark = parsedConfiguration,
    generatedBenchmarkOutputDir = Paths.get("./application/src/test/resources/generated"),
    generationResources = Paths.get("./application/src/test/resources/app/drivers")
  ).generate()

}
