package cloud.benchflow.benchmark.config

import cloud.benchflow.benchmark.config.benchflowbenchmark._
import cloud.benchflow.benchmark.config.collectors._
import cloud.benchflow.benchmark.config.docker.compose.DockerCompose
import cloud.benchflow.benchmark.heuristics.GenerationDefaults
import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv

import scala.xml.{Text, Node, Elem}
import scala.xml.transform.{RuleTransformer, RewriteRule}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 23/02/16.
  */
class FabanBenchmarkConfigurationBuilder(bb: BenchFlowBenchmark,
                                         benv: DriversMakerEnv,
                                         dd: DockerCompose) {

  //Faban doesn't like when there are newlines in the content of the tags,
  //so we remove them
  private val removeNewlinesRule = new RewriteRule {
    val minimizeEmpty = false
    override def transform(n: Node): Seq[Node] = n match {
      case Elem(prefix, label, attribs, scope, _, Text(content)) =>
        Elem(prefix, label, attribs, scope, minimizeEmpty, Text(content.trim))
      case other => other
    }
  }
  private object removeNewlines extends RuleTransformer(removeNewlinesRule)

  private val updateStatsRewriteRule = new RewriteRule {

    def addIntervalIfNotSpecified(statsChildren: Seq[Node]) = {
      statsChildren.find {
        case c: Elem if c.label == "interval" => true
        case _ => false
      } match {
        case Some(interval) => statsChildren
        case None => <interval>{GenerationDefaults.interval}</interval> :: statsChildren.toList
      }
    }

    val minimizeEmpty = false
    override def transform(n: Node): Seq[Node] = {
      n match {
        case stats: Elem if stats.label == "stats" =>
          Elem(stats.prefix, "stats", stats.attributes, stats.scope, minimizeEmpty, addIntervalIfNotSpecified(stats.child): _*)
        case other => other

      }
    }
  }

  private object insertIntervalIfNotExists extends RuleTransformer(updateStatsRewriteRule)

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
        <xml>{elem.child.map(addFabanNamespace)}</xml>.copy(label = ns + (if (ns != "") ":" else "") + elem.label,
                                                            attributes = elem.attributes)
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

  private def convertDriver(driver: Driver[_]): Elem =
    <driverConfig name={driver.getClass.getSimpleName}>
      {
        driver.properties match {
          case None => scala.xml.Null
          case Some(properties) => convert(properties)
        }
      }
    </driverConfig>

  private def convertDriver2(driver: Driver[_], agents: Set[(String, Int)]): Elem =
    <driverConfig name={driver.getClass.getSimpleName}>
      <fd:agents>{agents.map { case (host, numOfAgents) => s"$host:$numOfAgents" }.mkString(" ")}</fd:agents>
      {
        driver.properties match {
          case None => scala.xml.Null
          case Some(properties) => convert(properties).map(insertIntervalIfNotExists)
        }
      }
    </driverConfig>

  private def retrieveCollectors(bfConfig: BenchFlowConfig) = {

    def getCollectorAPI(collectorName: String) = {
      import scala.io.Source.fromFile
      val src = fromFile(benv.getBenchFlowServicesPath + s"/$collectorName.collector.yml").mkString
      CollectorAPI.fromYaml(src)
    }

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

    convertCollectors(uniqueCollectorsNames)
  }

  def resolvePrivatePort = {
    val targetServiceName = bb.sutConfiguration.targetService.name
    val targetService = dd.services.find(_.name.equalsIgnoreCase(targetServiceName)).get
    //.filter(_.name.equalsIgnoreCase(targetServiceName)).head
    targetService.getPrivatePort
  }

  private def getJavaOpts: String = {
    val jvm = benv.getHeuristics.jvm
    s"-Xmx${jvm.xmx(bb)}m -Xms${jvm.xms(bb)}m -XX:+DisableExplicitGC"
  }

  def build(trial: Trial) = {

    val scaleBalancer = benv.getHeuristics.scaleBalancer(bb)
    val agents = benv.getHeuristics.allocationHeuristic.agents(bb)
    val usedHosts = agents.values.reduce(_.union(_))

    removeNewlines(
      <xml>

        <jvmConfig xmlns:fh="http://faban.sunsource.net/ns/fabanharness">
          <fh:javaHome>/usr/lib/jvm/java7</fh:javaHome>
          <fh:jvmOptions>{ getJavaOpts }</fh:jvmOptions>
        </jvmConfig>

        <fa:runConfig definition={s"cloud.benchflow.benchmark.drivers.${bb.drivers.head.getClass.getSimpleName}"}
                      xmlns:fa="http://faban.sunsource.net/ns/faban"
                      xmlns:fh="http://faban.sunsource.net/ns/fabanharness"
                      xmlns="http://faban.sunsource.net/ns/fabandriver">
          <fh:description>{ bb.description }</fh:description>
          <fa:scale>{ scaleBalancer.scale }</fa:scale>
          <fh:timeSync>{ GenerationDefaults.timeSync }</fh:timeSync>
          <fa:hostConfig>
            <fa:host>{ usedHosts.map { case (host, numOfAgents) => s"$host" }.mkString(" ") }</fa:host>
            <fh:tools>NONE</fh:tools>
          </fa:hostConfig>

          {
            val xmlProps = convert(bb.properties).map(addFabanNamespace)
            xmlProps.map(insertIntervalIfNotExists)
            //++ bb.drivers.map(convertDriver).map(addFabanNamespace) }
          }

          { agents.map { case (d, hosts) => convertDriver2(d, hosts) } }

          <fa:runControl unit="time">
            <fa:rampUp>{ bb.execution.rampUp }</fa:rampUp>
            <fa:steadyState>{ bb.execution.steadyState }</fa:steadyState>
            <fa:rampDown>{ bb.execution.rampDown }</fa:rampDown>
          </fa:runControl>

          <threadStart>
            <delay>{ benv.getHeuristics.threadStart.delay(bb, usedHosts.size) }</delay>
            <simultaneous>{ benv.getHeuristics.threadStart.simultaneous(bb) }</simultaneous>
            <parallel>{ benv.getHeuristics.threadStart.parallel(bb) }</parallel>
          </threadStart>

          <sutConfiguration>
            <privatePort>{ resolvePrivatePort.get }</privatePort>
            <serviceName>{ bb.sutConfiguration.targetService.name }</serviceName>
            <endpoint>{ bb.sutConfiguration.targetService.endpoint }</endpoint>
          </sutConfiguration>

          <benchFlowServices>
              <benchFlowCompose>{ benv.getBenchFlowComposeAddress }</benchFlowCompose>
              <collectors>
                { retrieveCollectors(bb.sutConfiguration.bfConfig) }
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
