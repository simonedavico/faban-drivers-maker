package cloud.benchflow.benchmark.config.benchflowbenchmark

import net.jcazevedo.moultingyaml._

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 11/02/16.
  */
case class SutsNames(names: List[String])
object SutsNames {
  def apply(names: String*): SutsNames = SutsNames(names.toList)
}

/**
  * Possible types for a SUT
  */
sealed trait SutsType {
  def typeId: String
  def driverType: Class[_]
  def getType = driverType
}
object SutsType {
  def apply(sutsType: String) = sutsType.toLowerCase match {
    case WfMS.typeId => WfMS
    case Http.typeId => Http
    case _ => throw new Exception("Illegal value for field suts_type; possible values: wfms, http")
  }

  def getType(sutsType: String) = SutsType(sutsType).driverType
}
case object WfMS extends SutsType {
  val typeId = "wfms"
  val driverType = classOf[WfMSDriver]
}
case object Http extends SutsType {
  val typeId = "http"
  val driverType = classOf[HttpDriver]
}

/**
  * Http methods values
  */
sealed trait HttpMethod { def methodId: String }
object HttpMethod {
  def apply(method: String) = method.toUpperCase match {
    case Get.methodId => Get
    case Put.methodId => Put
    case Delete.methodId => Delete
    case Post.methodId => Post
    case _ => throw new Exception("Invalid http method specified.")
  }
}
case object Get extends HttpMethod {
  val methodId = "GET"
}
case object Put extends HttpMethod {
  val methodId = "PUT"
}
case object Delete extends HttpMethod {
  val methodId = "DELETE"
}
case object Post extends HttpMethod {
  val methodId = "POST"
}

case class Properties(properties: Map[String, Any])

/**
  * Possible operation types
  */
sealed abstract class Operation(val name: String, val data: Option[String])
case class HttpOperation(override val name: String,
                         endpoint: String,
                         override val data: Option[String] = None,
                         method: HttpMethod,
                         headers: Map[String, String] = Map()) extends Operation(name, data)
case class WfMSOperation(override val name: String, override val data: Option[String]) extends Operation(name, data)

/**
  * Possible driver types
  */
sealed abstract class Driver[A <: Operation](val properties: Option[Properties], val operations: Seq[A])
case class HttpDriver(override val properties: Option[Properties],
                      override val operations: Seq[HttpOperation]) extends Driver[HttpOperation](properties, operations)

sealed abstract class WfMSDriver(properties: Option[Properties],
                      operations: Seq[WfMSOperation]) extends Driver[WfMSOperation](properties, operations)
case class WfMSStartDriver(override val properties: Option[Properties],
                           override val operations: Seq[WfMSOperation]) extends WfMSDriver(properties, operations)
object WfMSDriver {

  def apply(t: String, properties: Option[Properties], operations: Seq[WfMSOperation]) = t match {
    case "start" => WfMSStartDriver(properties, operations)
    case _ => throw new Exception(s"Illegal driver identifier ${t}; possible values: start")
  }

}

case class TotalTrials(trials: Int)
case class Deploy(deploy: Map[String, String]) {
  def get(serviceName: String) = deploy.get(serviceName)
}
case class Binding(boundService: String, config: Option[Properties])
case class BenchFlowConfig(benchflow_config: Map[String, Seq[Binding]]) {
  def bindings(serviceName: String) = benchflow_config.getOrElse(serviceName, Seq())
}
case class TargetService(name: String, endpoint: String)
case class SutConfiguration(targetService: TargetService,
                            deploy: Deploy,
                            bfConfig: BenchFlowConfig)
case class BenchFlowBenchmark(name: String,
                              description: String,
                              suts_name: SutsNames,
                              suts_type: SutsType,
                              drivers: Seq[_ <: Driver[_ <: Operation]],
                              trials: TotalTrials,
                              properties: Properties,
                              `sut-configuration`: SutConfiguration)
{
  def getAliasForService(serviceName: String) = `sut-configuration`.deploy.get(serviceName)
  def getBindingsForService(serviceName: String) = `sut-configuration`.bfConfig.bindings(serviceName)
  def getBindingConfiguration(from: String, to: String): Option[Properties] =
    `sut-configuration`.bfConfig.bindings(from).find(b => b.boundService == to).flatMap(_.config)
}


//TODO: add drivers to BenchFlowBenchmark

object BenchFlowBenchmarkYamlProtocol extends DefaultYamlProtocol {

//  implicit val sutsTypeFormat = yamlFormat1(SutsType)
  implicit val deployFormat = yamlFormat1(Deploy)
  implicit val targetServiceFormat = yamlFormat2(TargetService)
  implicit val totalTrialsFormat = yamlFormat1(TotalTrials)

  implicit object SutsTypeYamlFormat extends YamlFormat[SutsType] {
    override def write(obj: SutsType): YamlValue = ???

    override def read(yaml: YamlValue): SutsType = {
      SutsType(yaml.asYamlObject.getFields(
        YamlString("suts_type")
      ).head.convertTo[String])
    }

  }

  implicit object SutsNamesYamlFormat extends YamlFormat[SutsNames] {
    override def write(obj: SutsNames): YamlValue = YamlObject()

    override def read(yaml: YamlValue): SutsNames = {
      val fields = yaml.asYamlObject.fields
      fields.seq.head match {
        case (YamlString("suts_name"), YamlArray(names)) =>
          SutsNames(names.map(name => name.convertTo[String]).toList)
        case (YamlString("sut_name"), YamlString(name)) =>
          SutsNames(name)
        case _ => throw DeserializationException("Unexpected format for field sut_name(s)")
      }
    }
  }

  implicit object PropertiesYamlFormat extends YamlFormat[Properties] {
    override def write(obj: Properties): YamlValue = ???

    private def toScalaPair(pair: (YamlValue, YamlValue)): (String, Any) = {
      val first = pair._1.convertTo[String]
      pair._2 match {
        case YamlString(value) => (first, value)
        case YamlObject(map) => (first, map.seq.map(toScalaPair))
        case YamlArray(values) => (first, values.toList.map(value => value.convertTo[String]))
        case _ => throw DeserializationException("Unexpected format for field properties")
      }
    }

    override def read(yaml: YamlValue): Properties = {
      val properties = yaml.asYamlObject.fields.head
      properties match {
        case (YamlString("properties"), YamlObject(props)) =>
          Properties(YamlObject(props).fields.map(toScalaPair))
        case _ => throw DeserializationException("Unexpected format for field properties")
      }
    }
  }


  implicit object BindingYamlFormat extends YamlFormat[Binding] {
    override def write(obj: Binding): YamlValue = ???

    override def read(yaml: YamlValue): Binding = {

      def readYamlWithConfig(yaml: YamlValue): Binding = {
        val binding = yaml.asYamlObject.fields.head
        val bfService = binding._1.convertTo[String]
        val props = binding._2.asYamlObject.getFields(YamlString("config")) match {
          case Seq(YamlObject(obj)) =>
            Some(YamlObject(YamlString("properties") -> YamlObject(obj)).convertTo[Properties])
          case _ => None
        }
        Binding(bfService, props)
      }

      yaml match {
        case YamlString(boundName) => Binding(boundName, None)
        case other => readYamlWithConfig(other)
      }

    }

  }

  implicit object HttpOperationYamlFormat extends YamlFormat[HttpOperation] {
    override def write(obj: HttpOperation): YamlValue = ???

    override def read(yaml: YamlValue): HttpOperation = {

      val fields = yaml.asYamlObject.fields
      val operationName = fields.seq.head._1.convertTo[String]
      val operationBody = fields.seq.head._2.asYamlObject
      val method = HttpMethod(operationBody.getFields(YamlString("method")).map(_.convertTo[String]).head)
      val headersMap = operationBody.getFields(YamlString("headers")).headOption match {
        case None => Map[String, String]()
        case Some(YamlObject(headers)) => YamlObject(headers).convertTo[Map[String, String]]
        case _ => throw new Exception("Invalid format for headers in operation " + operationName)
      }
      val data = operationBody.getFields(YamlString("data")).headOption.map(_.convertTo[String])
      val endpoint = operationBody.getFields(YamlString("endpoint")).head.convertTo[String]

      HttpOperation(name = operationName,
                    endpoint = endpoint,
                    method = method,
                    headers = headersMap,
                    data = data)
    }
  }

  implicit object WfMSOperationYamlFormat extends YamlFormat[WfMSOperation] {
    override def write(obj: WfMSOperation): YamlValue = ???

    override def read(yaml: YamlValue): WfMSOperation = {
      val fields = yaml.asYamlObject.fields
      val operationName = fields.seq.head._1.convertTo[String]
      val operationBody = fields.seq.head._2.asYamlObject
      val data = operationBody.getFields(YamlString("data")).headOption.map(_.convertTo[String])
      WfMSOperation(name = operationName, data = data)
    }
  }



  //TODO: figure out how to make drivers yaml format generic
  implicit object HttpDriverYamlFormat extends YamlFormat[HttpDriver] {
    override def write(obj: HttpDriver): YamlValue = ???

    override def read(yaml: YamlValue): HttpDriver = {

      val fields = yaml.asYamlObject.fields
      val driverName = fields.head._1.convertTo[String]
      val driverBody = fields.head._2.asYamlObject

      val driverProperties = driverBody.getFields(YamlString("properties")).headOption match {
        case None => None
        case Some(properties) => Some(YamlObject(YamlString("properties") -> properties).convertTo[Properties])
      }

      val driverOperations = driverBody.getFields(YamlString("operations")).head match {
        case YamlArray(ops) => ops.map(_.convertTo[HttpOperation])
      }

      HttpDriver(properties = driverProperties,
                 operations = driverOperations)
    }
  }

  implicit object WfMSDriverYamlFormat extends YamlFormat[WfMSDriver] {
    override def write(obj: WfMSDriver): YamlValue = ???

    override def read(yaml: YamlValue): WfMSDriver = {

      val fields = yaml.asYamlObject.fields
      val driverName = fields.head._1.convertTo[String]
      val driverBody = fields.head._2.asYamlObject

      val driverProperties = driverBody.getFields(YamlString("properties")).headOption match {
        case None => None
        case Some(properties) => Some(YamlObject(YamlString("properties") -> properties).convertTo[Properties])
      }

      val driverOperations = driverBody.getFields(YamlString("operations")).head match {
        case YamlArray(ops) => ops.map(_.convertTo[WfMSOperation])
      }

      WfMSDriver(t = driverName,
        properties = driverProperties,
        operations = driverOperations)
    }
  }



  implicit object BenchFlowConfigFormat extends YamlFormat[BenchFlowConfig] {
    override def write(obj: BenchFlowConfig): YamlValue = ???

    override def read(yaml: YamlValue): BenchFlowConfig = {
      val bfConfig = yaml.asYamlObject.fields.head
      val bindings = bfConfig._2.asYamlObject.fields

      BenchFlowConfig(
        bindings.map(binding => binding match {
          case (YamlString(sName), YamlArray(bound)) =>
            (sName, bound.map(binding => binding.convertTo[Binding]))
          case _ => throw DeserializationException("Unexpected format for field benchflow-config")
        })
      )

    }
  }

  implicit object SutConfigurationFormat extends YamlFormat[SutConfiguration] {
    override def write(obj: SutConfiguration): YamlValue = ???

    override def read(yaml: YamlValue): SutConfiguration = {
      val sutConfig = yaml.asYamlObject.fields.head
      val sections = sutConfig._2.asYamlObject.fields.toMap
      val deployKey = YamlString("deploy")
      val bfConfigKey = YamlString("benchflow-config")
      val deploy = YamlObject(deployKey -> sections.get(deployKey).get).convertTo[Deploy]
      val bfConfig = YamlObject(bfConfigKey -> sections.get(bfConfigKey).get).convertTo[BenchFlowConfig]
      val targetService = sections.get(YamlString("target-service")).get.convertTo[TargetService]
      SutConfiguration(deploy = deploy, bfConfig = bfConfig, targetService = targetService)
    }
  }

  implicit object BenchFlowBenchmarkConfigurationFormat extends YamlFormat[BenchFlowBenchmark] {
    override def write(obj: BenchFlowBenchmark): YamlValue = ???

    override def read(yaml: YamlValue): BenchFlowBenchmark = {

      //TODO: add suts_name/sut_name compatibility

      def getObject(key: String)(implicit obj: Map[YamlValue, YamlValue]) =
        YamlObject(YamlString(key) -> obj.get(YamlString(key)).get)

      implicit val bfBmark = yaml.asYamlObject.fields.toMap
      val sutName = getObject("sut_name").convertTo[SutsNames]
      val sutType = getObject("suts_type").convertTo[SutsType]

      //TODO: figure out if it's possible to avoid matching again on sut type here
      val drivers = sutType match {
        case WfMS => bfBmark.get(YamlString("drivers")).get.asInstanceOf[YamlArray].elements.map(d => d.convertTo[WfMSDriver])
        case Http => bfBmark.get(YamlString("drivers")).get.asInstanceOf[YamlArray].elements.map(d => d.convertTo[HttpDriver])
        case _ => throw new Exception("Illegal value for suts_type field.")
      }

      val name = bfBmark.get(YamlString("benchmark_name")).get.convertTo[String]
      val description = bfBmark.get(YamlString("description")).get.convertTo[String]
      val properties = getObject("properties").convertTo[Properties]
      val sutConfig = getObject("sut-configuration").convertTo[SutConfiguration]
      val trials = getObject("trials").convertTo[TotalTrials]

      BenchFlowBenchmark(
        name = name,
        description = description,
        suts_name = sutName,
        suts_type = sutType,
        drivers = drivers,
        properties = properties,
        trials = trials,
        `sut-configuration` = sutConfig
      )
    }
  }

}

object BenchFlowBenchmark {

  def fromYaml(yaml: String): BenchFlowBenchmark = {
    import BenchFlowBenchmarkYamlProtocol._
    yaml.stripMargin.parseYaml.convertTo[BenchFlowBenchmark]
  }

}


