package cloud.benchflow.config

import cloud.benchflow.config
import cloud.benchflow.config.benchflowbenchmark._
import cloud.benchflow.config.collectors.CollectorAPI
import cloud.benchflow.config.docker.compose.DockerCompose
import cloud.benchflow.driversmaker.configurations.FabanDefaults
import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.driversmaker.utils.env.DriversMakerBenchFlowEnv

import scala.xml.{Text, Node, Elem}
import scala.xml.transform.{RuleTransformer, RewriteRule}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 23/02/16.
  */
class FabanBenchmarkConfigurationBuilder(bb: BenchFlowBenchmark, benv: DriversMakerBenchFlowEnv,
                                         fd: FabanDefaults, dd: DockerCompose) {

  private val removeNewlinesRule = new RewriteRule {
    val minimizeEmpty = false
    override def transform(n: Node): Seq[Node] = n match {
      case Elem(prefix, label, attribs, scope, _, Text(content)) =>
        Elem(prefix, label, attribs, scope, minimizeEmpty, Text(content.trim))
      case other => other
    }
  }
  private object removeNewlines extends RuleTransformer(removeNewlinesRule)

  private def propertyToNamespace =
    Map(
      "hostConfig" -> "fa",
      "hostPorts" -> "fa",
      "host" -> "fa",
      "tools" -> "fh",
      "scale" -> "fa",
      "runControl" -> "fa",
      "rampUp" -> "fa",
      "steadyState" -> "fa",
      "rampDown" -> "fa",
      "cpus" -> "fh",
      "enabled" -> "fh",
      "timeSync" -> "fh"
    )

  private def addFabanNamespace(elem: Node): Node = {
    elem match {
      case elem: Elem =>
        val ns = propertyToNamespace.getOrElse(elem.label, "")
        <xml>{elem.child.map(addFabanNamespace)}</xml>.copy(label = ns + (if (ns != "") ":" else "") + elem.label, attributes = elem.attributes)
      case _ => elem
    }
  }

  private def convert(property: (String, Any)): Elem =
    <xml>{

      property._2 match {
        case map: Map[String, Any] => map.map(convert)
        case nestedList: List[Map[String, Any]] => nestedList.map(n => n.map(convert))
        case plainList: List[_] => plainList
        case other => other
      }

    }</xml>.copy(label = property._1)


  private def convert(properties: Properties): Iterable[Elem] =
    properties.properties.map(convert)

  private def convert(driver: Driver): Elem =
    <driverConfig name={driver.name.substring(driver.name.lastIndexOf(".")+1)}>{ convert(driver.properties) }</driverConfig>

  private def retrieveCollectors(bfConfig: BenchFlowConfig) = {

    def getCollectorAPI(collectorName: String) = {
      import scala.io.Source.fromFile
      val src = fromFile(benv.getBenchFlowServicesPath + s"/$collectorName.collector.yml").mkString
      CollectorAPI.fromYaml(src)
    }

//
//    def convert(collector: Binding): Elem = {
//      val api = getCollectorAPI(collector.boundService)
//      <collector>
//        {
//          api.start.map(s => <start>{s}</start>).getOrElse(scala.xml.Null) ++
//          <stop>{api.stop}</stop>
//        }
//      </collector>.copy(label = collector.boundService)
//    }

    def convertCollector(collectorName: String): Elem = {

      def getBindings(a: String, b: Seq[Binding]): Option[String] = {
        b.map(_.boundService).contains(collectorName) match {
          case true => Some(a)
          case _ => None
        }
      }

      val bindings = bfConfig.benchflow_config.flatMap(t => getBindings(t._1, t._2))

      val api = getCollectorAPI(collectorName)
      <collector>
        {
          api.start.map(s => <start>{s}</start>).getOrElse(scala.xml.Null) ++
          <stop>{api.stop}</stop>
          <privatePort>{api.privatePort}</privatePort>
          <bindings>{bindings mkString "," }</bindings>
        }
      </collector>.copy(label = collectorName)
    }


    def convertCollectors(collectors: Set[String]) = collectors.map(convertCollector)
    val collectors = bfConfig.benchflow_config.values.foldLeft(Set[Binding]())((v1,v2) => v1 union v2.toSet)
    val uniqueCollectorsNames = collectors.map(_.boundService)

//    convertCollectors(collectors)
    convertCollectors(uniqueCollectorsNames)
  }

  def resolvePrivatePort = {
    val targetServiceName = bb.`sut-configuration`.targetService.name
    val targetService = dd.services.filter(_.name.equalsIgnoreCase(targetServiceName)).head
    targetService.getPrivatePort
  }

  def build(trial: Trial) = {

    removeNewlines(
      <xml>

        <jvmConfig xmlns:fh="http://faban.sunsource.net/ns/fabanharness">
          <fh:javaHome>{fd.getJavaHome}</fh:javaHome>
          <fh:jvmOptions>{fd.getJavaOpts}</fh:jvmOptions>
        </jvmConfig>

        <fa:runConfig definition="cloud.benchflow.wfmsbenchmark.driver.WfMSBenchmarkDriver"
                      xmlns:fa="http://faban.sunsource.net/ns/faban"
                      xmlns:fh="http://faban.sunsource.net/ns/fabanharness"
                      xmlns="http://faban.sunsource.net/ns/fabandriver">
           <fh:description>{ bb.description }</fh:description>

           { convert(bb.properties).map(addFabanNamespace) ++ bb.drivers.map(convert).map(addFabanNamespace) }

          <sutConfiguration>
            <privatePort>{ resolvePrivatePort.get }</privatePort>
            <serviceName>{bb.`sut-configuration`.targetService.name}</serviceName>
            <endpoint>{bb.`sut-configuration`.targetService.endpoint}</endpoint>
          </sutConfiguration>

          <benchFlowServices>
              <benchFlowCompose>{ benv.getBenchFlowComposePath }</benchFlowCompose>
              <collectors>
                { retrieveCollectors(bb.`sut-configuration`.bfConfig) }
              </collectors>
              <monitors>
                <mysql>http://192.168.41.105:9303/status</mysql>
              </monitors>
           </benchFlowServices>

           <benchFlowRunConfiguration>
             <trialId>{trial.getTrialId}</trialId>
           </benchFlowRunConfiguration>

        </fa:runConfig>

      </xml>.copy(label = bb.name)
    )

  }

}
