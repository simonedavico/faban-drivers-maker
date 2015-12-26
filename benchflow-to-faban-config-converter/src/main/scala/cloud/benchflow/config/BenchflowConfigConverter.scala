package cloud.benchflow.config

import java.io.InputStream

/**
  * @author: Simone D'avico
  */
//generates xml from the output of the YAML parser
object XMLGenerator {

  def toXML(mapping: (String, Any)): scala.xml.Elem = {
    <xml>{

      mapping._2 match {
        case s: String => s
        case map: Map[String, Any] => toXML(map)
        case list: List[Map[String, Any]] => list map toXML
      }

      }</xml>.copy(label = mapping._1)
  }

  def toXML(map: Map[String, Any]): List[scala.xml.Elem] = {
    map map toXML toList
  }

}

//transform intermediate XML into Faban compliant XML
object FabanXMLTransformer {

  def propertyToNamespace =
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
      "rampDown" -> "fa"
    )

  private def addFabanNamespace(elem: scala.xml.Node): scala.xml.Elem = {
    val ns = propertyToNamespace.getOrElse(elem.label, "")
    elem.asInstanceOf[scala.xml.Elem].copy(label = ns + ":" + elem.label)
  }

  private def transformDriverConfig(elem: scala.xml.Node): scala.xml.Elem =
    <driverConfig name={elem.label}>{elem.child map addFabanNamespace}</driverConfig>

  def apply(elem: scala.xml.Elem, javaHome: String, javaOpts: String): scala.xml.Elem = {
    (elem \ "drivers" theSeq).head match {
      case <drivers>{drivers @ _*}</drivers> =>
        <xml>
          <jvmConfig xmlns="http://faban.sunsource.net/ns/fabanharness">
            <javaHome>{javaHome}</javaHome>
            <jvmOptions>{javaOpts}</jvmOptions>
          </jvmConfig>
          <fa:runConfig definition={elem.label}
                        xmlns:fa="http://faban.sunsource.net/ns/faban"
                        xmlns:fh="http://faban.sunsource.net/ns/fabanharness"
                        xmlns="http://faban.sunsource.net/ns/fabandriver">
            {drivers map transformDriverConfig}
          </fa:runConfig>
        </xml>.copy(label = elem.label)
      case _ => throw new Exception //TODO: throw meaningful exception
    }
  }

}

//the interface to the business logic
class BenchflowConfigConverter(val javaHome: String, val javaOpts: String) {

  def from(in: InputStream): scala.xml.Elem = {

    import YAMLParser._
    import XMLGenerator._

    implicit def anyToMap(value: Any): Map[String, Any] =
      value.asInstanceOf[Map[String, Any]]

    val yaml = io.Source.fromInputStream(in).mkString

    val xml = parse(yaml) match {
      case Success(r, b) => toXML(r)
      case Failure(msg, n) => throw new Exception //TODO: throw meaningful exception
      case Error(msg, n) => throw new Exception //TODO: as above
    }

    FabanXMLTransformer(xml head, javaHome, javaOpts)
  }

}


