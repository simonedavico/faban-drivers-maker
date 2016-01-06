package cloud.benchflow.config

import java.io.{FileInputStream, InputStream}
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._
import scala.xml.{PrettyPrinter, Elem, Node}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 26/12/15.
  */
//generates xml from the output of the YAML parser
object XMLGenerator {

  def toXML(mapping: (String, Any)): Elem = {
    <xml>{

      mapping._2 match {
        case map: java.util.Map[String, Any] => toXML(map)
        case list: java.util.List[java.util.Map[String, Any]] => list.asScala.toList.map(toXML)
        case value @ _ => value
      }

      }</xml>.copy(label = mapping._1)
  }

  def toXML(map: java.util.Map[String, Any]): Iterable[Elem] = {
    map.asScala.toMap map toXML
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
    <driverConfig name={elem.label}>{elem.child map addFabanNamespace("")}</driverConfig>

  def apply(elem: Elem, javaHome: String, javaOpts: String): Elem = {
    (elem \\ "drivers" theSeq).head match {
      case <drivers>{drivers @ _*}</drivers> =>
         <xml>
          <jvmConfig xmlns="http://faban.sunsource.net/ns/fabanharness">
            <javaHome>{javaHome}</javaHome>
            <jvmOptions>{javaOpts}</jvmOptions>
          </jvmConfig>
           { elem.child.filter(child => child.label != "drivers") map addFabanNamespace("") }
          <fa:runConfig definition={drivers.head.label}
                        xmlns:fa="http://faban.sunsource.net/ns/faban"
                        xmlns:fh="http://faban.sunsource.net/ns/fabanharness"
                        xmlns:fd="http://faban.sunsource.net/ns/fabandriver">
           { drivers map transformDriverConfig }
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
class BenchFlowConfigConverter(val javaHome: String, val javaOpts: String) {

//  private val configMap: java.util.Map[String, String] =
//    (new Yaml load new FileInputStream(BenchFlowConfigConverter.configPath))
//              .asInstanceOf[java.util.Map[String, String]]

  private def convert(in: InputStream): Elem = {
    import XMLGenerator._
    //val (javaHome, javaOpts) = (configMap.get("java.home"), configMap.get("java.opts"))
    val yaml = scala.io.Source.fromInputStream(in).mkString
    val map = (new Yaml load yaml).asInstanceOf[java.util.Map[String, Any]]
    FabanXML(toXML(map).head, javaHome, javaOpts)
  }

  def from(in: InputStream): String = {
    new PrettyPrinter(60, 2).format(convert(in))
  }

}


