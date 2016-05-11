package cloud.benchflow.benchmark.config.benchflowbenchmark

import net.jcazevedo.moultingyaml._

//TODO: turn all generic exceptions to DeserializationException

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 11/02/16.
  */
case class SutsName(name: String)

trait Version { def isCompatible(other: Version): Boolean }
object Version {

  case class NumberedVersion(v: Int) extends Ordered[NumberedVersion] with Version {
    override def compare(that: NumberedVersion): Int = this.v - that.v
    override def toString = s"v$v"

    override def isCompatible(other: Version): Boolean = {

      other match {
        case numbered: NumberedVersion => this == numbered
        case ranged: RangedVersion => ranged.isInRange(this)
        case _ => false
      }

    }
  }
  case class RangedVersion(low: Int, high: Int) extends Version {
    def isInRange(v: NumberedVersion) = NumberedVersion(low) <= v && v <= NumberedVersion(high)
    override def toString = s"v$low-v$high"

    override def isCompatible(other: Version): Boolean = {

      other match {
        case numbered: NumberedVersion => this.isInRange(numbered)
        case ranged: RangedVersion => ranged.low  == this.low && this.high == ranged.high
        case _ => false
      }

    }
  }
  case class StringVersion(v: String) extends Version {
    override def toString = v

    override def isCompatible(other: Version): Boolean = {

      other match {
        case stringed: StringVersion => stringed.v == this.v
        case _ => false
      }

    }
  }

  private val numberedVersionPattern = "v([0-9]+)".r
  private val rangedVersionPattern = "v([0-9]+)-v([0-9]+)".r

  def apply(v: String) = v match {
    case numberedVersionPattern(version) => NumberedVersion(version.toInt)
    case rangedVersionPattern(low, high) => RangedVersion(low.toInt, high.toInt)
    case _ => StringVersion(v)
  }

}




/**
  * Possible types for a SUT
  */
sealed trait SutsType
object SutsType {

  def apply(sutsType: String): SutsType = sutsType.toLowerCase match {
    case "wfms" => WfMS
    case "http" => Http
    case _ => throw new Exception("Illegal value for field suts_type; possible values: wfms, http")
  }
}
case object WfMS extends SutsType
case object Http extends SutsType

case class Sut(name: String, version: Version, sutsType: SutsType)

/**
  * Http methods values
  */
sealed trait HttpMethod
object HttpMethod {
  def apply(method: String) = method.toLowerCase match {
    case "get" => Get
    case "put" => Put
    case "delete" => Delete
    case "post" => Post
    case _ => throw new Exception("Invalid http method specified.")
  }
}
case object Get extends HttpMethod
case object Put extends HttpMethod
case object Delete extends HttpMethod
case object Post extends HttpMethod

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
  * Possible mixes
  */
//TODO: fix deviation to be Double
sealed abstract class Mix(deviation: Option[Int])

//This mix maintains the state of execution.
//It chooses the next operation based on the current operation and a given probability ratio.
case class MatrixMixRow(row: Seq[Double])
case class MatrixMix(rows: Seq[MatrixMixRow], deviation: Option[Int]) extends Mix(deviation)

//This mix randomly chooses the next operation to execute based on given probability for the mix.
case class FlatMix(opsMix: Seq[Double], deviation: Option[Int]) extends Mix(deviation)

//The fixed sequence does what it says. There is no randomness. The operations are called in sequence.
case class FixedSequenceMix(sequence: Seq[String], deviation: Option[Int]) extends Mix(deviation)

//This mix allows random selection of fixed sequences (as opposed to random selection of an operation in FlatMix).
case class FlatSequenceMixRow(row: Seq[String])
case class FlatSequenceMix(opsMix: Seq[Double],
                           rows: Seq[FlatSequenceMixRow],
                           deviation: Option[Int]) extends Mix(deviation)

sealed trait DriverMetric //TODO: possible values will be: ops/sec, req/s(?)

case class DriverConfiguration(max90th: Option[Double], mix: Option[Mix])


/**
  * Possible driver types
  */
sealed abstract class Driver[A <: Operation](val properties: Option[Properties],
                                             val operations: Seq[A],
                                             val configuration: Option[DriverConfiguration])
case class HttpDriver(override val properties: Option[Properties],
                      override val operations: Seq[HttpOperation],
                      override val configuration: Option[DriverConfiguration])
  extends Driver[HttpOperation](properties, operations, configuration)

sealed abstract class WfMSDriver(properties: Option[Properties],
                      operations: Seq[WfMSOperation],
                      configuration: Option[DriverConfiguration])
  extends Driver[WfMSOperation](properties, operations, configuration)

case class WfMSStartDriver(override val properties: Option[Properties],
                           override val operations: Seq[WfMSOperation],
                           override val configuration: Option[DriverConfiguration])
  extends WfMSDriver(properties, operations, configuration)

object WfMSDriver {
  def apply(t: String,
            properties: Option[Properties],
            operations: Seq[WfMSOperation],
            configuration: Option[DriverConfiguration]) = t match {
    case "start" => WfMSStartDriver(properties, operations, configuration)
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
case class VirtualUsers(virtualUsers: Int)
case class BenchFlowBenchmark(name: String,
                              description: String,
//                              sutsName: SutsName,
                              sut: Sut,
                              //sutsType: SutsType,
                              virtualUsers: VirtualUsers,
                              drivers: Seq[_ <: Driver[_ <: Operation]],
                              trials: TotalTrials,
                              properties: Properties,
                              sutConfiguration: SutConfiguration)
{
  def getAliasForService(serviceName: String) = sutConfiguration.deploy.get(serviceName)
  def getBindingsForService(serviceName: String) = sutConfiguration.bfConfig.bindings(serviceName)
  def getBindingConfiguration(from: String, to: String): Option[Properties] =
    sutConfiguration.bfConfig.bindings(from).find(b => b.boundService == to).flatMap(_.config)
}

object BenchFlowBenchmarkYamlProtocol extends DefaultYamlProtocol {

//  implicit val sutsTypeFormat = yamlFormat1(SutsType)
  implicit val deployFormat = yamlFormat1(Deploy)
  implicit val targetServiceFormat = yamlFormat2(TargetService)
  implicit val totalTrialsFormat = yamlFormat1(TotalTrials)
  implicit val virtualUsersFormat = yamlFormat1(VirtualUsers)

  implicit object SutsTypeYamlFormat extends YamlFormat[SutsType] {
    override def write(obj: SutsType): YamlValue = ???

    override def read(yaml: YamlValue): SutsType = {
      SutsType(yaml.asYamlObject.getFields(
        YamlString("suts_type")
      ).head.convertTo[String])
    }

  }

  implicit object SutsNamesYamlFormat extends YamlFormat[SutsName] {
    override def write(obj: SutsName): YamlValue = YamlObject()

    override def read(yaml: YamlValue): SutsName = {

      yaml.asYamlObject.getFields(YamlString("suts_name")).head match {
        case YamlString(name) => SutsName(name)
        case _ => throw new Exception("Illegal value specified for field suts_name")
      }

    }
  }

  implicit object SutYamlFormat extends YamlFormat[Sut] {
    override def write(obj: Sut): YamlValue = ???

    override def read(yaml: YamlValue): Sut = {
      val sutName = yaml.asYamlObject.fields.get(YamlString("name")) match {
        case None => throw new Exception("No name specified in sut definition")
        case Some(YamlString(name)) => name
      }

      val version = yaml.asYamlObject.fields.get(YamlString("version")) match {
        case None => throw new Exception("No version specified in sut definition")
        case Some(YamlString(v)) => Version(v)
      }

      val sutsType = yaml.asYamlObject.fields.get(YamlString("type")) match {
        case None => throw new Exception("No type specified in sut definition")
        case Some(YamlString(t)) => SutsType(t)
      }

      Sut(sutName, version, sutsType)
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
      val data = method match {
        case Post | Put => operationBody.getFields(YamlString("data")).headOption.map(_.convertTo[String])
        case _ => None //force no data for requests that don't have a body
      }
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

  implicit object MatrixMixRowYamlFormat extends YamlFormat[MatrixMixRow] {
    override def write(obj: MatrixMixRow): YamlValue = ???

    override def read(yaml: YamlValue): MatrixMixRow = {
      yaml.asInstanceOf[YamlArray] match {
        case YamlArray(elems) => MatrixMixRow(elems.map(_.convertTo[Double]))
        case _ => throw new Exception("Incorrect format for matrix mix row")
      }
    }
  }

  implicit object FlatSequenceMixRowYamlFormat extends YamlFormat[FlatSequenceMixRow] {
    override def write(obj: FlatSequenceMixRow): YamlValue = ???

    override def read(yaml: YamlValue): FlatSequenceMixRow = {
      yaml.asInstanceOf[YamlArray] match {
        case YamlArray(elems) => FlatSequenceMixRow(elems.map(_.convertTo[String]))
        case _ => throw new Exception("Incorrect format for flatSequence mix row")
      }
    }
  }

  implicit object FlatSequenceMixYamlFormat extends YamlFormat[FlatSequenceMix] {
    override def write(obj: FlatSequenceMix): YamlValue = ???

    override def read(yaml: YamlValue): FlatSequenceMix = {

      val flatSequenceBody = yaml.asYamlObject.fields.get(YamlString("flatSequence")).get
      val flat = flatSequenceBody.asYamlObject.getFields(
        YamlString("flat")
      ).head match {
        case YamlArray(flatProbs) => flatProbs.map(_.convertTo[Double])
        case _ => throw new Exception("Missing or incorrect format for sequence mix flat probabilities field")
      }

      val sequences = flatSequenceBody.asYamlObject.getFields(
        YamlString("sequences")
      ).head match {
        case YamlArray(rows) => rows.map(_.convertTo[FlatSequenceMixRow])
        case _ => throw new Exception("Missing or incorrect format for sequence mix sequences field")
      }

      val deviation = yaml.asYamlObject.getFields(
        YamlString("deviation")
      ).headOption match {
        case Some(YamlNumber(dev: Int)) => Some(dev)
        case None => None
      }

      FlatSequenceMix(deviation = deviation, rows = sequences, opsMix = flat)
    }
  }

  implicit object MatrixMixYamlFormat extends YamlFormat[MatrixMix] {
    override def write(obj: MatrixMix): YamlValue = ???

    override def read(yaml: YamlValue): MatrixMix = {
      val matrixRows = yaml.asYamlObject.fields.get(YamlString("matrix")).get match {
        case YamlArray(rows) => rows.map(_.convertTo[MatrixMixRow])
        case _ => throw new Exception("Incorrect format for matrix mix")
      }

      val deviation = yaml.asYamlObject.getFields(
        YamlString("deviation")
      ).headOption match {
        case Some(YamlNumber(dev: Int)) => Some(dev)
        case None => None
      }

      MatrixMix(matrixRows, deviation)
    }
  }

  implicit object FlatMixYamlFormat extends YamlFormat[FlatMix] {
    override def write(obj: FlatMix): YamlValue = ???

    override def read(yaml: YamlValue): FlatMix = {

      val deviation = yaml.asYamlObject.getFields(
        YamlString("deviation")
      ).headOption match {
        case Some(YamlNumber(dev: Int)) => Some(dev)
        case None => None
      }

      yaml.asYamlObject.getFields(
        YamlString("flat")
      ).head match {
        case YamlArray(probs) => FlatMix(probs.map(_.convertTo[Double]), deviation)
        case _ => throw new Exception("Unexpected format for flat mix")
      }

    }
  }

  implicit object SequenceMixYamlFormat extends YamlFormat[FixedSequenceMix] {
    override def write(obj: FixedSequenceMix): YamlValue = ???

    override def read(yaml: YamlValue): FixedSequenceMix = {

      val deviation = yaml.asYamlObject.getFields(
        YamlString("deviation")
      ).headOption match {
        case Some(YamlNumber(dev: Int)) => Some(dev)
        case None => None
      }

      yaml.asYamlObject.getFields(
        YamlString("fixedSequence")
      ).head match {
        case YamlArray(sequence) => FixedSequenceMix(sequence.map(_.convertTo[String]), deviation)
        case _ => throw new Exception("Unexpected format for sequence mix")
      }

    }
  }

  implicit object DriverConfigurationYamlFormat extends YamlFormat[DriverConfiguration] {
    override def write(obj: DriverConfiguration): YamlValue = ???

    override def read(yaml: YamlValue): DriverConfiguration = {

      def generateMix(yamlMix: YamlValue): Mix = {

        val mixMap = yamlMix.asYamlObject.fields
        Seq("matrix", "flat", "fixedSequence", "flatSequence")
          .map(mixType => mixMap.get(YamlString(mixType))) match {
          case Seq(None, None, Some(seq), None) => yamlMix.convertTo[FixedSequenceMix]
          case Seq(None, Some(flat), None, None) =>  yamlMix.convertTo[FlatMix]
          case Seq(Some(matrix), None, None, None) =>  yamlMix.convertTo[MatrixMix]
          case Seq(None, None, None, Some(flatSequence)) => yamlMix.convertTo[FlatSequenceMix]
        }

      }

//      val threadPerScale = yaml.asYamlObject.fields.get(YamlString("threadPerScale")).map(_.convertTo[Int])
      val max90th = yaml.asYamlObject.fields.get(YamlString("max90th")).map(_.convertTo[Double])//.flatMap(_.convertTo[Double])

      yaml.asYamlObject.fields.get(YamlString("mix")) match {
        case None => DriverConfiguration(mix = None, max90th = max90th)
        case Some(mix) => DriverConfiguration(mix = Some(generateMix(mix)),
                                                         max90th = max90th)
      }

    }
  }


  //TODO: figure out how to make drivers yaml format generic (may not be possible)
  implicit object HttpDriverYamlFormat extends YamlFormat[HttpDriver] {
    override def write(obj: HttpDriver): YamlValue = ???

    override def read(yaml: YamlValue): HttpDriver = {

      val fields = yaml.asYamlObject.fields
      //val driverName = fields.head._1.convertTo[String]
      val driverBody = fields.head._2.asYamlObject

      val driverProperties = driverBody.getFields(YamlString("properties")).headOption match {
        case None => None
        case Some(properties) => Some(YamlObject(YamlString("properties") -> properties).convertTo[Properties])
      }

      val driverOperations = driverBody.getFields(YamlString("operations")).head match {
        case YamlArray(ops) => ops.map(_.convertTo[HttpOperation])
        case _ => throw new Exception("invalid format; drivers section of benchflow-benchmark.yml has to be a list")
      }

      val driverConfiguration = driverBody.getFields(YamlString("configuration")).headOption match {
        case None => None
        case Some(driverConfig) => Some(driverConfig.convertTo[DriverConfiguration])
      }

      HttpDriver(properties = driverProperties,
                 operations = driverOperations,
                 configuration = driverConfiguration)
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
        case _ => throw new Exception("invalid format; drivers section of benchflow-benchmark.yml has to be a list")
      }

      val driverConfiguration = driverBody.getFields(YamlString("configuration")).headOption match {
        case None => None
        case Some(driverConfig) => Some(driverConfig.convertTo[DriverConfiguration])
      }

      WfMSDriver(t = driverName,
        properties = driverProperties,
        operations = driverOperations,
        configuration = driverConfiguration)
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

      def getObject(key: String)(implicit obj: Map[YamlValue, YamlValue]) =
        YamlObject(YamlString(key) -> obj.get(YamlString(key)).get)

      implicit val bfBmark = yaml.asYamlObject.fields.toMap
//      val sutName = getObject("suts_name").convertTo[SutsName]
      val sut = yaml.asYamlObject.fields.get(YamlString("sut")).get.convertTo[Sut]
//      val sutType = getObject("suts_type").convertTo[SutsType]

      //TODO: figure out if it's possible to avoid matching again on sut type here
      val drivers = sut.sutsType match {
        case WfMS => bfBmark.get(YamlString("drivers")).get.asInstanceOf[YamlArray].elements.map(d => d.convertTo[WfMSDriver])
        case Http => bfBmark.get(YamlString("drivers")).get.asInstanceOf[YamlArray].elements.map(d => d.convertTo[HttpDriver])
        case _ => throw new Exception("Illegal value for suts_type field.")
      }

      val name = bfBmark.get(YamlString("benchmark_name")).get.convertTo[String]
      val description = bfBmark.get(YamlString("description")).get.convertTo[String]
      val properties = getObject("properties").convertTo[Properties]
      val sutConfig = getObject("sut-configuration").convertTo[SutConfiguration]
      val trials = getObject("trials").convertTo[TotalTrials]
      val virtualUsers = getObject("virtualUsers").convertTo[VirtualUsers]

      BenchFlowBenchmark(
        name = name,
        description = description,
        sut = sut,
        //sutsType = sutType,
        drivers = drivers,
        properties = properties,
        trials = trials,
        sutConfiguration = sutConfig,
        virtualUsers = virtualUsers
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


