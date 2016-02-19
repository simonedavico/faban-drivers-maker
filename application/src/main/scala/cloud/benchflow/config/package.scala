package cloud.benchflow

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 10/02/16.
  */
//TODO: fork moultingyaml, set protected visibility on YamlValue's asYamlObject function
//TODO: and all subclasses (YamlObject, ...)
//TODO: then define a pretty print that escapes values with double quotes
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
  case class Service(name: String,
                     image: Option[Image] = None,
                     containerName: Option[ContainerName] = None,
                     command: Option[Command] = None,
                     environment: Option[Environment] = None,
                     volumes: Option[Volumes] = None,
                     ports: Option[Ports] = None,
                     expose: Option[Expose] = None) {
    /***
      * Returns the port, taking into account the formats "ip:port" and "port"
      */
    def getPort: Option[String] = {
      val pattern = "(.*):([0-9]*)".r
      ports.flatMap(_.ports.head match {
        case pattern(ip, p) => Some(p)
        case other => ports.map(_.ports.head)
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

            )
        )
      }

      override def read(value: YamlValue) =  {
        val fields = value.asYamlObject.fields
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

            val expose = params.fields.get(YamlString("expose"))
            match {
              case Some(YamlArray(exp)) =>
                Some(Expose(exp.map(e => e.convertTo[Int])))
              case _ => None
            }

            Service(serviceName,
              image = image,
              containerName = cname,
              command = command,
              environment = environment,
              volumes = volumes,
              ports = ports,
              expose = expose
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

    def resolve = ???
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
  //templates for variable resolution
//  abstract class BFVar(val s: String) { v =>
//    def resolve(implicit r: Resolver[v.type]) = r.resolve(v)
//  }
//
//  class BFEnvVar(override val s: String) extends BFVar(s) {
//
//  }
//
//  abstract class Source[T <: BFVar] {
//    def resolve(v: T)
//  }
//
//  abstract class Resolver[T <: BFVar] {
//    def resolve(v: T)(implicit r: Resolver[T])
//  }

}
