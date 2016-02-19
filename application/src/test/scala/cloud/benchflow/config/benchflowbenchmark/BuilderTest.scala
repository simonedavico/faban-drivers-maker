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
      |    container_name: mycontainername
      |bubu:
      |    image: bubuImage
      |    environment: [ bubuvar ]
    """.stripMargin
  val completeConfiguration =
    """sut_name: camunda
      |suts_type: WfMS
      |benchmark_name: myBenchmark
      |description: configuration for testing
      |properties:
      |    one: two
      |    three:
      |        four: five
      |    six: [ seven ]
      |sut-configuration:
      |    target-service:
      |        name: camunda
      |        endpoint: /engine-rest
      |    deploy:
      |        camunda: alias1
      |        bubu: alias2
      |    benchflow-config:
      |        camunda: [ stats ]
      |        foo:
      |            - bar:
      |                 config:
      |                      one: two
    """.stripMargin


  val bfEnv = new BenchFlowEnv("./application/src/test/resources/app/config.yml",
                               "./application/src/test/resources/app/benchflow-services")
  val builder = new BenchFlowConfigurationBuilder(dockerCompose, completeConfiguration, bfEnv)
  println(builder.build)
}
