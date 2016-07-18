package cloud.benchflow.experiment.config.docker.compose.deploymentdescriptor

import net.jcazevedo.moultingyaml._
import cloud.benchflow.experiment.config.docker.compose.Service
import DockerComposeYamlProtocol._

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 05/07/16.
  */

case class NetworkConfig(driver: String)
case class Networks(nets: Map[String, NetworkConfig])

case class DockerCompose(services: Map[String, Service],
                         version: Int,
                         networks: Option[Networks]) {
  this.toYaml.prettyPrint
}
object DockerCompose {
  def fromYaml(yaml: String): DockerCompose = yaml.parseYaml.convertTo[DockerCompose]
}
