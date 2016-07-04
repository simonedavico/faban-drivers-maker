package cloud.benchflow.benchmark.config.benchflowbenchmark

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 11/02/16.
  */
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
sealed abstract class Mix(deviation: Option[Double])

//This mix maintains the state of execution.
//It chooses the next operation based on the current operation and a given probability ratio.
case class MatrixMixRow(row: Seq[Double])
case class MatrixMix(rows: Seq[MatrixMixRow], deviation: Option[Double]) extends Mix(deviation)

//This mix randomly chooses the next operation to execute based on given probability for the mix.
case class FlatMix(opsMix: Seq[Double], deviation: Option[Double]) extends Mix(deviation)

//The fixed sequence does what it says. There is no randomness. The operations are called in sequence.
case class FixedSequenceMix(sequence: Seq[String], deviation: Option[Double]) extends Mix(deviation)

//This mix allows random selection of fixed sequences (as opposed to random selection of an operation in FlatMix).
case class FlatSequenceMixRow(row: Seq[String])
case class FlatSequenceMix(opsMix: Seq[Double],
                           rows: Seq[FlatSequenceMixRow],
                           deviation: Option[Double]) extends Mix(deviation)

sealed trait DriverMetric //TODO: possible values will be: ops/sec, req/s(?)

case class DriverConfiguration(max90th: Option[Double], mix: Option[Mix], popularity: Option[Float])


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
    case _ => throw new Exception(s"Illegal driver identifier $t; possible values: start")
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

case class Execution(rampUp: Int, steadyState: Int, rampDown: Int)


case class BenchFlowBenchmark(name: String,
                              description: String,
                              sut: Sut,
                              virtualUsers: VirtualUsers,
//                              drivers: Seq[_ <: Driver[_ <: Operation]],
                              drivers: Seq[Driver[_ <: Operation]],
                              trials: TotalTrials,
                              execution: Execution,
                              properties: Properties,
                              sutConfiguration: SutConfiguration)
{
  def getAliasForService(serviceName: String) = sutConfiguration.deploy.get(serviceName)
  def getBindingsForService(serviceName: String) = sutConfiguration.bfConfig.bindings(serviceName)
  def getBindingConfiguration(from: String, to: String): Option[Properties] =
    sutConfiguration.bfConfig.bindings(from).find(b => b.boundService == to).flatMap(_.config)
}
object BenchFlowBenchmark {

  def fromYaml(yaml: String): BenchFlowBenchmark = {
    import net.jcazevedo.moultingyaml._
    import BenchFlowBenchmarkYamlProtocol._
    yaml.stripMargin.parseYaml.convertTo[BenchFlowBenchmark]
  }


}


