package cloud.benchflow.config.docker.compose
import cloud.benchflow.config._
import net.jcazevedo.moultingyaml._
import cloud.benchflow.config.benchflowbenchmark._

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 05/02/16.
  */
object YamlTest extends App {
  val dc = DockerCompose(Service("camunda", image = Some(Image("ciao"))))
//  println(dc)

  val tests = List(
    "http://${BENCHFLOW_CIAO}/ciao/${BENCHFLOW_ADDIO}",
    "http://${BENCHFLOW_CIAO}/ciao/${bla}",
    "${blablabla}/ciao/${fofofo}",
    "http://",
    "${ahahahah}",
    "${BENCHFLOW}",
    ""
  )

  import cloud.benchflow.config.benchflowbenchmark.BenchFlowBenchmarkYamlProtocol._

  println("""suts_name: [a, b, c]""".stripMargin.parseYaml.convertTo[SutsNames])
  println("""sut_name: a""".stripMargin.parseYaml.convertTo[SutsNames])
  println("""suts_type: WfMS""".stripMargin.parseYaml.convertTo[SutsType])

  println(
    """properties:
      |    ciao: ciao
      |    boh:
      |       addio: addio
      |    foo:
      |       - uno
    """.stripMargin.parseYaml.convertTo[Properties])

  println(
    """deploy:
      |    foo: lisa1
      |    bar: lisa2
      |    foobar: neha
    """.stripMargin.parseYaml.convertTo[Deploy])

  println(
    """bound:
      |    config:
      |         uno: due
      |         tre:
      |            quattro: cinque
    """.stripMargin.parseYaml.convertTo[Binding])

  println("bound".parseYaml.convertTo[Binding])


  println("""benchflow-config:
    |            service1:
    |                - benchflow-service1:
    |                    config:
    |                        uno: due
    |                        tre:
    |                            quattro: cinque
    |                - benchflow-service2
    |            service2: [ benchflow-service3, benchflow-service4 ]"""
    .stripMargin.parseYaml.convertTo[BenchFlowConfig])

  println(
    """sut-configuration:
      |    target-service:
      |        name: camunda
      |        endpoint: /engine-rest
      |    deploy:
      |        camunda: lisa1
      |    benchflow-config:
      |        camunda: [ stats, mysqldump ]
      |        foo:
      |            - bar:
      |                 config:
      |                      madre: padre
    """.stripMargin.parseYaml.convertTo[SutConfiguration])

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
      |    benchflow-config:
      |        camunda: [stats, mysqldump]
      |        foo:
      |            - bar:
      |                 config:
      |                      madre: padre
    """

  println(BenchFlowBenchmark.fromYaml(completeConfiguration))


}
