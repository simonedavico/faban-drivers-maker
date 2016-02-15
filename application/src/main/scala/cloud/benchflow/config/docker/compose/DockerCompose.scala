package cloud.benchflow.config.docker.compose

import cloud.benchflow.config._
import net.jcazevedo.moultingyaml._
import ServiceYamlProtocol._

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 05/02/16.
  */
object DockerComposeYamlProtocol extends DefaultYamlProtocol {

  implicit object DockerComposeYamlFormat extends YamlFormat[DockerCompose] {

    override def write(obj: DockerCompose) = {
      YamlObject(
        obj.services.map(service => (service.name.toYaml,
          service.toYaml.asYamlObject.fields.get(YamlString(service.name)).get)).toMap
      )
    }

    override def read(yaml: YamlValue) = {
      def toService(s: (YamlValue, YamlValue)): Service = YamlObject(s).convertTo[Service]
      DockerCompose(yaml.asYamlObject.fields.map(toService).toList)
    }
  }

}

case class DockerCompose(services: List[Service]) {

  import DockerComposeYamlProtocol._

  def ++(newServices: Seq[Service]): DockerCompose =
    DockerCompose(services ++ newServices)

  override def toString: String = {
    this.toYaml.prettyPrint
  }
}
object DockerCompose {
  def apply(services: Service*): DockerCompose = DockerCompose(services.toList)
  def fromYaml(yaml: String): DockerCompose = {
    import DockerComposeYamlProtocol._
    yaml.stripMargin.parseYaml.convertTo[DockerCompose]
  }
}




