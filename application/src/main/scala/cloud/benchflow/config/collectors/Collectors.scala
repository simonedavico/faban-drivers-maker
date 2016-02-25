package cloud.benchflow.config.collectors

import net.jcazevedo.moultingyaml._

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 24/02/16.
  */
case class CollectorAPI(start: Option[String], stop: String)

object CollectorYamlProtocol extends DefaultYamlProtocol {

  implicit object CollectorAPIYamlFormat extends YamlFormat[CollectorAPI] {
    override def write(obj: CollectorAPI): YamlValue = ???

    override def read(yaml: YamlValue): CollectorAPI = {
      val endpoints = yaml.asYamlObject.fields.get(YamlString("endpoints")).get.asYamlObject.fields
      val start = endpoints.get(YamlString("start")).map(_.convertTo[String])
      val stop = endpoints.get(YamlString("stop")).map(_.convertTo[String]).get
      CollectorAPI(start, stop)
    }
  }

}

object CollectorAPI {
  def fromYaml(yaml: String) = {
    import CollectorYamlProtocol._
    yaml.stripMargin.parseYaml.convertTo[CollectorAPI]
  }
}
