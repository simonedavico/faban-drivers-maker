package cloud.benchflow.config.benchflowbenchmark

import cloud.benchflow.config.BenchFlowConfigurationBuilder
import cloud.benchflow.driversmaker.utils.BenchFlowEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 13/02/16.
  */
object BuilderTest extends App {

  val dockerCompose =
    """camunda:
      |    image: camundaImage
      |    environment: [ camundavar ]
      |bubu:
      |    image: bubuImage
    """.stripMargin
  val completeConfiguration =
    """sut_name: camunda
      |suts_type: WfMS
      |benchmark_name: myBenchmark
      |description: configuration for testing
      |properties:
      |    uno: due
      |    tre:
      |        quattro: cinque
      |    sei: [ sette ]
      |sut-configuration:
      |    target-service:
      |        name: camunda
      |        endpoint: /engine-rest
      |    deploy:
      |        camunda: lisa1
      |        bubu: lisa2
      |    benchflow-config:
      |        camunda: [ stats ]
      |        foo:
      |            - bar:
      |                 config:
      |                      madre: padre
    """.stripMargin


  val bfEnv = new BenchFlowEnv("./application/src/test/resources/app/config.yml",
                               "./application/src/test/resources/app/benchflow-services")
  val builder = new BenchFlowConfigurationBuilder(bfEnv = bfEnv,
                                                  dockercompose = dockerCompose,
                                                  benchFlowBenchmark = completeConfiguration)

  builder.build()
  println(builder.dockerCompose)
}
