package cloud.benchflow.benchmark

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 10/02/16.
  */
package object config {

  import scala.util.parsing.combinator._
  import net.jcazevedo.moultingyaml._

  case class ContainerName(container_name: String)
  case class Command(command: String)
  case class Environment(environment: Seq[String]) {
    def :+(envVar: String): Environment = Environment(environment :+ envVar)
  }
  case class Volumes(volumes: Seq[String])
  case class Ports(ports: Seq[String])
  case class Image(image: String)
  case class Expose(expose: Seq[Int])
  case class Network(net: String)
  case class ExtraHosts(extra_hosts: Seq[String])
  //TODO: add cpuset
  //TODO: add mem_limit
  case class Service(name: String,
                     image: Option[Image] = None,
                     containerName: Option[ContainerName] = None,
                     command: Option[Command] = None,
                     environment: Option[Environment] = None,
                     volumes: Option[Volumes] = None,
                     ports: Option[Ports] = None,
                     net: Option[Network] = None,
                     extra_hosts: Option[ExtraHosts] = None,
                     expose: Option[Expose] = None) {

//    private val ipAndPorts = "(.*):([0-9]+):([0-9]+)".r
//    private val ipAndSinglePort = "(.*):([0-9]+)".r
    private val ipAndPorts = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}:([0-9]+):([0-9]+)".r
    private val ipAndSinglePort = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}:([0-9]+)".r
    private val onlyPorts = "([0-9]+):([0-9]+)".r
    private val onlySinglePort = "([0-9]+)".r

    def getPorts: Option[String] = {
        ports.flatMap(_.ports.head match {
          case ipAndPorts(ip, publicPort, privatePort) => Some(s"$publicPort:$privatePort")
          case onlyPorts(publicPort, privatePort) => Some(s"$publicPort:$privatePort")
          case ipAndSinglePort(ip, publicPort) => Some(publicPort)
          case onlySinglePort(publicPort) => Some(publicPort)
        })
    }

    def getPrivatePort: Option[String] = {
      ports.flatMap(_.ports.head match {
        case ipAndPorts(ip, publicPort, privatePort) => Some(s"$privatePort")
        case onlyPorts(publicPort, privatePort) => Some(s"$privatePort")
        case ipAndSinglePort(ip, publicPort) => None
        case onlySinglePort(publicPort) => None
      })
    }
  }



  object ServiceYamlProtocol extends DefaultYamlProtocol {

    implicit val imageFormat = yamlFormat1(Image)
    implicit val containerFormat = yamlFormat1(ContainerName)
    implicit val commandFormat = yamlFormat1(Command)
    implicit val environmentFormat = yamlFormat1(Environment)
    implicit val volumesFormat = yamlFormat1(Volumes)
    implicit val portsFormat = yamlFormat1(Ports)
    implicit val exposeFormat = yamlFormat1(Expose)
    implicit val networkFormat = yamlFormat1(Network)
    implicit val extraHostsFormat = yamlFormat1(ExtraHosts)

    implicit object ServiceYamlFormat extends YamlFormat[Service] {

      override def write(c: Service) = {
        val emptyMap = Map[YamlValue, YamlValue]()
        YamlObject(
          YamlString(c.name) ->
            YamlObject(

              (c.image match {
                case Some(_) => c.image.toYaml.asYamlObject.fields
                case _ => emptyMap
              })

              ++

              (c.containerName match {
                case Some(_) => c.containerName.toYaml.asYamlObject.fields
                case _ => emptyMap
              })

              ++

              (c.command match {
                case Some(_) => c.command.toYaml.asYamlObject.fields
                case _ => emptyMap
              })

              ++

              (c.environment match {
                case Some(_) => c.environment.toYaml.asYamlObject.fields
                case _ => emptyMap
              })

              ++

              (c.volumes match {
                case Some(_) => c.volumes.toYaml.asYamlObject.fields
                case _ => emptyMap
              })

              ++

              (c.ports match {
                case Some(_) => c.ports.toYaml.asYamlObject.fields
                case _ => emptyMap
              })

              ++

              (c.expose match {
                case Some(_) => c.expose.toYaml.asYamlObject.fields
                case _ => emptyMap
              })

              ++

              (c.net match {
                case Some(_) => c.net.toYaml.asYamlObject.fields
                case _ => emptyMap
              })

              ++

              (c.extra_hosts match {
                case Some(_) => c.extra_hosts.toYaml.asYamlObject.fields
                case _ => emptyMap
              })

            )
        )
      }

      override def read(value: YamlValue) =  {
        val fields = value.asYamlObject.fields.filter(f => f._1 != YamlString("endpoints"))
        fields.seq.head match {
          case (YamlString(serviceName), content) =>
            val params = content.asYamlObject

            val cname = params.fields.get(YamlString("container_name"))
            match {
              case Some(YamlString(container_name)) => Some(ContainerName(container_name))
              case _ => None
            }

            val command = params.fields.get(YamlString("command"))
            match {
              case Some(YamlString(cmd)) => Some(Command(cmd))
              case _ => None
            }

            val image = params.fields.get(YamlString("image"))
            match {
              case Some(YamlString(img)) => Some(Image(img))
              case _ => None
            }

            val environment = params.fields.get(YamlString("environment"))
            match {
              case Some(YamlArray(vars)) =>
                Some(Environment(vars.map(v => v.convertTo[String])))
              case _ => None
            }

            val volumes = params.fields.get(YamlString("volumes"))
            match {
              case Some(YamlArray(vols)) =>
                Some(Volumes(vols.map(v => v.convertTo[String])))
              case _ => None
            }

            val ports = params.fields.get(YamlString("ports"))
            match {
              case Some(YamlArray(ps)) =>
                Some(Ports(ps.map(p => p.convertTo[String])))
              case _ => None
            }

            val extra_hosts = params.fields.get(YamlString("extra_hosts"))
            match {
              case Some(YamlArray(eh)) =>
                Some(ExtraHosts(eh.map(h => h.convertTo[String])))
              case _ => None
            }

            val expose = params.fields.get(YamlString("expose"))
            match {
              case Some(YamlArray(exp)) =>
                Some(Expose(exp.map(e => e.convertTo[Int])))
              case _ => None
            }

            val network = params.fields.get(YamlString("net"))
            match {
              case Some(YamlString(net)) => Some(Network(net))
              case _ => None
            }

            Service(serviceName,
              image = image,
              containerName = cname,
              command = command,
              environment = environment,
              volumes = volumes,
              ports = ports,
              expose = expose,
              net = network,
              extra_hosts = extra_hosts
            )
          case _ => throw DeserializationException("Invalid Docker compose file")
        }
      }

    }

  }

  object Service {
    def fromYaml(yaml: String): Service = {
      import ServiceYamlProtocol._
      yaml.parseYaml.convertTo[Service]
    }
  }


  implicit class BenchFlowEnvString(val s: String) extends AnyVal {
    def findBenchFlowVars = BenchFlowVariableFinder.findIn(s)
  }

  class BenchFlowVariableParser extends RegexParsers {

    //opening delimiter for a variable, ${
    val openVar = """\$\{""".r

    //closing delimiter for a variable, }
    val closeVar = """\}""".r

    val benchFlowPrefix = "BENCHFLOW_"

    //a benchflow variable, i.e. ${BENCHFLOW_VAR_1}
    //REMINDER: double escape is for string interpolation
    val benchflowVar = openVar ~> s"""$benchFlowPrefix([^\\}])+""".r <~ closeVar

    //a string without benchflow vars to resolve, i.e
    //"foobar", or "${foobar}"
    //REMINDER: the double $ is an escaped $
    val string = s"""((?!\\$$\\{$benchFlowPrefix).)*""".r

    //an expression containing benchflow variables
    val exprWithVars = ((string?) ~> benchflowVar <~ (string?))+

  }

  object BenchFlowVariableFinder extends BenchFlowVariableParser { parser =>
    def findIn(s: String) = parseAll(exprWithVars, s) match {
      case parser.NoSuccess(_, _) => None
      case parser.Success(result, _) => Some(result)
    }
  }


}
