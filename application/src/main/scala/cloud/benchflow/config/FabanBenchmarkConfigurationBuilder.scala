package cloud.benchflow.config

import cloud.benchflow.config.benchflowbenchmark._
import cloud.benchflow.config.collectors.CollectorAPI
import cloud.benchflow.driversmaker.configurations.FabanDefaults
import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.driversmaker.utils.BenchFlowEnv

import scala.xml.{Text, Node, Elem}
import scala.xml.transform.{RuleTransformer, RewriteRule}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 23/02/16.
  */
class FabanBenchmarkConfigurationBuilder(bb: BenchFlowBenchmark, benv: BenchFlowEnv, fd: FabanDefaults) {

  private val removeNewlinesRule = new RewriteRule {
    val minimizeEmpty = false
    override def transform(n: Node): Seq[Node] = n match {
      case Elem(prefix, label, attribs, scope, _, Text(content)) =>
        Elem(prefix, label, attribs, scope, minimizeEmpty, Text(content.trim))
      case other => other
    }
  }
  private object removeNewlines extends RuleTransformer(removeNewlinesRule)

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
    <driverConfig name={driver.name}>{ convert(driver.properties) }</driverConfig>

  private def retrieveCollectors(bfConfig: BenchFlowConfig) = {

    def getCollectorAPI(collectorName: String) = {
      import scala.io.Source.fromFile
      val src = fromFile(benv.getBenchFlowServicesPath + s"/$collectorName.collector.yml").mkString
      CollectorAPI.fromYaml(src)
    }


    def convert(collector: Binding): Elem = {
      val api = getCollectorAPI(collector.boundService)
      <collector>
        {
          api.start.map(s => <start>{s}</start>).getOrElse(scala.xml.Null) ++
          <stop>{api.stop}</stop>
        }
      </collector>.copy(label = collector.boundService)
    }


    def convertCollectors(collectors: Set[Binding]) = collectors.map(convert)

    val collectors = bfConfig.benchflow_config.values.foldLeft(Set[Binding]())((v1,v2) => v1 union v2.toSet)
    convertCollectors(collectors)
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

           { convert(bb.properties) ++ bb.drivers.map(convert) }

          <sutConfiguration>
            <serviceName>{bb.`sut-configuration`.targetService.name}</serviceName>
            <endpoint>{bb.`sut-configuration`.targetService.endpoint}</endpoint>
          </sutConfiguration>

          <benchFlowServices>
              <benchFlowCompose>{ benv.getBenchFlowComposePath }</benchFlowCompose>
              <collectors>
                { retrieveCollectors(bb.`sut-configuration`.bfConfig) }
              </collectors>
           </benchFlowServices>

           <benchFlowRunConfiguration>
             <trialId>{trial.getTrialId}</trialId>
           </benchFlowRunConfiguration>

        </fa:runConfig>

      </xml>.copy(label = bb.name)
    )

  }

}
