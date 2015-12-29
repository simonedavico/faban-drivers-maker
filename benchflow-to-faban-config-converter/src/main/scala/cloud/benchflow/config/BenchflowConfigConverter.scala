package cloud.benchflow.config

import java.io.{FileInputStream, InputStream}
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._
import scala.xml.{Elem, Node}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 26/12/15.
  */
//generates xml from the output of the YAML parser
object XMLGenerator {

  def toXML(mapping: (String, Any)) = {
    <xml>{

      mapping._2 match {
        case map: java.util.Map[String, Any] => toXML(map)
        case list: java.util.List[java.util.Map[String, Any]] => list.asScala.toList.map(toXML)
        case value @ _ => value
      }

      }</xml>.copy(label = mapping._1)
  }

  def toXML(map: java.util.Map[String, Any]) = {
    (map.asScala.toMap map toXML).toList
  }

}

//transform intermediate XML into Faban compliant XML
object FabanXML {

  private def propertyToNamespace =
    Map(
      "description" -> "fh",
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

  private def addFabanNamespace(defaultNS: String)(elem: Node): Node = {
    elem match {
      case elem: Elem =>
        val ns = propertyToNamespace.getOrElse(elem.label, defaultNS)
        <xml>{elem.child.map(addFabanNamespace(defaultNS))}</xml>.copy(label = ns + (if (ns != "") ":" else "") + elem.label)
      case _ => elem
    }
  }

  private def transformDriverConfig(elem: Node): Node =
    <fd:driverConfig name={elem.label}>{elem.child map addFabanNamespace("fd")}</fd:driverConfig>

  def apply(elem: scala.xml.Elem, javaHome: String, javaOpts: String): scala.xml.Elem = {
    (elem \\ "drivers" theSeq).head match {
      case <drivers>{drivers @ _*}</drivers> =>
         <xml>
          <jvmConfig xmlns="http://faban.sunsource.net/ns/fabanharness">
            <javaHome>{javaHome}</javaHome>
            <jvmOptions>{javaOpts}</jvmOptions>
          </jvmConfig>
           { elem.child.filter(node => node.label != "drivers") map addFabanNamespace("") }
          <fa:runConfig definition={drivers.head.label}
                        xmlns:fa="http://faban.sunsource.net/ns/faban"
                        xmlns:fh="http://faban.sunsource.net/ns/fabanharness"
                        xmlns:fd="http://faban.sunsource.net/ns/fabandriver">
            {drivers map transformDriverConfig}
          </fa:runConfig>
        </xml>.copy(label = elem.label)
      case _ => throw new Exception //TODO: throw meaningful exception
    }
  }

}

object BenchFlowConfigConverter {
  private val configPath = "./benchflow-to-faban-config-converter/src/main/resources/config.yml"
}

//the interface to the business logic
class BenchFlowConfigConverter {

  private val configMap =
    (new Yaml load new FileInputStream(BenchFlowConfigConverter.configPath))
              .asInstanceOf[java.util.Map[String, String]]

  def from(in: InputStream): scala.xml.Elem = {

    import XMLGenerator._

    val javaHome = configMap.get("java.home")
    val javaOpts = configMap.get("java.opts")
    val yaml = io.Source.fromInputStream(in).mkString
    val map = (new Yaml load yaml).asInstanceOf[java.util.Map[String, Any]]
    FabanXML(toXML(map).head, javaHome, javaOpts)
  }

}


