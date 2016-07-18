package cloud.benchflow.config.docker.compose

import net.jcazevedo.moultingyaml._

import cloud.benchflow.experiment.config._
import cloud.benchflow.experiment.config.benchflowservices.collectors._
import cloud.benchflow.experiment.config.docker.compose._
import cloud.benchflow.experiment.config.experimentdescriptor._

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 05/02/16.
  */
object YamlTest extends App {
//  val dc = DockerCompose(Service("camunda", image = Some(Image("ciao")), net = Some(Network("host")), extra_hosts = Some(ExtraHosts(Seq("bla")))))
//  println(dc)

  val tests = List(
    "http://${BENCHFLOW_HELLO}/ciao/${BENCHFLOW_GOODBYE}",
    "http://${BENCHFLOW_HELLO}/ciao/${bla}",
    "${blablabla}/ciao/${fofofo}",
    "http://",
    "${ahahahah}",
    "${BENCHFLOW}",
    ""
  )

  import cloud.benchflow.experiment.config.experimentdescriptor.BenchFlowExperimentYamlProtocol._
  import cloud.benchflow.experiment.config.benchflowservices.collectors.CollectorYamlProtocol._

//  println("""suts_name: [a, b, c]""".stripMargin.parseYaml.convertTo[SutsName])
//  println("""sut_name: a""".stripMargin.parseYaml.convertTo[SutsName])
//  println("""suts_type: WfMS""".stripMargin.parseYaml.convertTo[SutsType])

  println(
    """properties:
      |    hello: hello
      |    boh:
      |       bye: bye
      |    foo:
      |       - one
    """.stripMargin.parseYaml.convertTo[Properties])

  println(
    """deploy:
      |    foo: alias1
      |    bar: alias2
      |    foobar: alias3
    """.stripMargin.parseYaml.convertTo[Deploy])

  println(
    """bound:
      |    config:
      |         one: two
      |         three:
      |            four: five
    """.stripMargin.parseYaml.convertTo[Binding])

  println("bound".parseYaml.convertTo[Binding])


  println("""benchflow-config:
    |            service1:
    |                - benchflow-service1:
    |                    config:
    |                        one: two
    |                        three:
    |                            four: five
    |                - benchflow-service2
    |            service2: [ benchflow-service3, benchflow-service4 ]"""
    .stripMargin.parseYaml.convertTo[BenchFlowConfig])

  println(
    """sut-configuration:
      |    target-service:
      |        name: camunda
      |        endpoint: /engine-rest
      |    deploy:
      |        camunda: alias1
      |    benchflow-config:
      |        camunda: [ stats, mysql ]
      |        foo:
      |            - bar:
      |                 config:
      |                      one: two
    """.stripMargin.parseYaml.convertTo[SutConfiguration])

  val completeConfiguration =
    """sut_name: camunda
      |suts_type: WfMS
      |benchmark_name: myBenchmark
      |description: configuration for testing
      |trials: 5
      |properties:
      |    one: two
      |    three:
      |        four: five
      |    six: [ seven ]
      |drivers:
      |    - driver1:
      |          key1: val1
      |          key2: val2
      |sut-configuration:
      |    target-service:
      |        name: camunda
      |        endpoint: /engine-rest
      |    deploy:
      |        camunda: lisa1
      |    benchflow-config:
      |        camunda: [stats, mysql]
      |        foo:
      |            - bar:
      |                 config:
      |                      one: two
    """

//  println(BenchFlowBenchmark.fromYaml(completeConfiguration))

//  val driver =
//    """fooDriver:
//      |    prop1:
//      |        nested1:
//      |            - list
//      |    prop2: val2
//    """.stripMargin.parseYaml.convertTo[Driver]

//  println(driver)

  val collectorAPI =
    """endpoints:
      |    start: /start
      |    stop: /stop
    """.stripMargin.parseYaml.convertTo[CollectorAPI]

  println(collectorAPI)

  val benchflowCollector =
    """stats:
      |    foo: bar
      |endpoints:
      |    start: /start
      |    stop: /stop
    """.stripMargin.parseYaml.convertTo[CollectorAPI]

  println(benchflowCollector)


}
