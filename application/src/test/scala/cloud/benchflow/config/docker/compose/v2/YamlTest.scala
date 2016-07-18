package cloud.benchflow.config.docker.compose.v2

import cloud.benchflow.experiment.config.docker.compose.Service
import cloud.benchflow.experiment.config.docker.compose.deploymentdescriptor.DockerCompose

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 05/07/16.
  */
object YamlTest extends App {

  import net.jcazevedo.moultingyaml._
  import cloud.benchflow.experiment.config.docker.compose.ServiceYamlProtocol._
  import cloud.benchflow.experiment.config.docker.compose.deploymentdescriptor.DockerComposeYamlProtocol._

  val yamlService =
    """
      |camunda:
      |  image: camunda_image
      |  cpuset: 0,1,2,3
      |  mem_limit: 5g
      |  environment:
      |    - constraint:node==bull
      |    - MY_VAR=5
      |    - MY_OTHER_VAR=http://google.com
    """.stripMargin

  println(yamlService.parseYaml.convertTo[Service])

  val dockerComposeV2 =
    """
      |version: 2
      |services:
      |  camunda:
      |    image: camunda_image
      |    mem_limit: 5g
      |    environment:
      |      - MY_VAR=5
      |  other.service:
      |     image: other.image
      |networks:
      |  foo:
      |    driver: custom
    """.stripMargin

  val parsedCompose = dockerComposeV2.parseYaml.convertTo[DockerCompose]

  println(parsedCompose)
  println(parsedCompose.toYaml.prettyPrint)







}
