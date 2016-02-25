package cloud.benchflow.config.converter

import java.io.InputStream
import org.yaml.snakeyaml.Yaml
import scala.xml.transform.{RuleTransformer, RewriteRule}
import util.Properties

import scala.collection.JavaConverters._
import scala.xml._

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

  private def specialCases = List("drivers", "sut", "runControl")

  private def addFabanNamespace(defaultNS: String)(elem: Node): Node = {
    elem match {
      case elem: Elem =>
        val ns = propertyToNamespace.getOrElse(elem.label, defaultNS)
        <xml>{elem.child.map(addFabanNamespace(defaultNS))}</xml>.copy(label = ns + (if (ns != "") ":" else "") + elem.label)
      case _ => elem
    }
  }

  private def transformDriverConfig(elem: Node): Node =
    <driverConfig name={elem.label.substring(elem.label.lastIndexOf(".")+1)}>
      {elem.child map addFabanNamespace("")}
    </driverConfig>


  def apply(elem: Elem, javaHome: String, javaOpts: String): Elem = {
    (elem \ "drivers" theSeq).head match {
      case <drivers>{drivers @ _*}</drivers> =>
        <xml>
          <jvmConfig xmlns:fh="http://faban.sunsource.net/ns/fabanharness">
            <fh:javaHome>{javaHome}</fh:javaHome>
            <fh:jvmOptions>{javaOpts}</fh:jvmOptions>
          </jvmConfig>
          <fa:runConfig definition={drivers.head.label}
                        xmlns:fa="http://faban.sunsource.net/ns/faban"
                        xmlns:fh="http://faban.sunsource.net/ns/fabanharness"
                        xmlns="http://faban.sunsource.net/ns/fabandriver">
            { elem.child.filter(child => !specialCases.contains(child.label)) map addFabanNamespace("") }

            {
              val runControl = (elem \ "runControl" theSeq).headOption
              runControl match {
                case Some(<runControl>{ content @ _* }</runControl>) =>
                  <fa:runControl unit="time">
                    { content map addFabanNamespace("") }
                  </fa:runControl>
                case None => None
              }
            }

            { drivers map transformDriverConfig }
          </fa:runConfig>
          {
            (elem \ "sut" theSeq).head match {
              case <sut>{suts @ _*}</sut> => suts
            }
          }
        </xml>.copy(label = elem.label)
      case _ => throw new Exception //TODO: throw meaningful exception
    }
  }

}

//the interface to the business logic
class BenchFlowBenchmarkConfigConverter(val javaHome: String, val javaOpts: String) {

  import XMLGenerator._

  private val removeNewlinesRule = new RewriteRule {
    val minimizeEmpty = false
    override def transform(n: Node): Seq[Node] = n match {
      case Elem(prefix, label, attribs, scope, _, Text(content)) =>
        Elem(prefix, label, attribs, scope, minimizeEmpty, Text(content.trim))
      case other => other
    }
  }
  private object removeNewlines extends RuleTransformer(removeNewlinesRule)

  private def convert(in: InputStream): Elem = {
    val map = (new Yaml load in).asInstanceOf[java.util.Map[String, Any]]
    FabanXML(toXML(map).head, javaHome, javaOpts)
  }

  private def convert(in: String): Elem = {
    val map = (new Yaml load in).asInstanceOf[java.util.Map[String, Any]]
    FabanXML(toXML(map).head, javaHome, javaOpts)
  }

  def convertAndStringify(in: InputStream): String = {
    val sb = new StringBuilder ++= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + Properties.lineSeparator
    val result = removeNewlines(convert(in)).head
    (sb ++= new PrettyPrinter(400, 2).format(result)).toString
  }

  def convertAndStringify(in : String): String = {
    val sb = new StringBuilder ++= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + Properties.lineSeparator
    val result = removeNewlines(convert(in)).head
    (sb ++= new PrettyPrinter(400, 2).format(result)).toString
  }

}


