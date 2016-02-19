package cloud.benchflow.config

import cloud.benchflow.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.config.docker.compose.DockerCompose
import cloud.benchflow.driversmaker.utils.BenchFlowEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 16/02/16.
  */
class BenchFlowConfigurationBuilder(val dcyaml: String, bbyaml: String, val benv: BenchFlowEnv) {

  val bb = BenchFlowBenchmark.fromYaml(bbyaml)

  type DCTransformer[T] = T => DockerCompose => DockerCompose
  type ServiceTransformer[T] = T => Service => Service
  type BenchFlowServiceTransformer[T] = T => ServiceTransformer[BenchFlowBenchmark]

  abstract class BenchFlowVariable(val name: String) {
    type Source
    def resolve(implicit source: Source): String
  }

  case class BenchFlowEnvVariable(override val name: String) extends BenchFlowVariable(name) {
    type Source = BenchFlowEnv
    override def resolve(implicit source: Source): String = source.getVariable[String](s"BENCHFLOW_$name")
  }
  object BenchFlowEnvVariable {
    val prefix = "(BENCHFLOW_ENV_)(.*)".r
  }

  case class BenchFlowBoundServiceVariable(override val name: String) extends BenchFlowVariable(name) {
    override type Source = Service

    override def resolve(implicit source: Source): String = {
      name match {
        case "IP" => bb.getAliasForService(source.name).getOrElse(BenchFlowBoundServiceVariable.prefix + "_IP")
        case "PORT" => "port" //best way to get the port?
        case "CONTAINER_NAME" => source.containerName.map(_.container_name)
                                       .getOrElse(BenchFlowBoundServiceVariable + "_CONTAINER_NAME")
        case other => s"BENCHFLOW_BENCHMARK_BOUNDSERVICE_$other"
      }
    }
  }
  object BenchFlowBoundServiceVariable {
    val prefix = "(BENCHFLOW_BENCHMARK_BOUNDSERVICE_)(.*)".r
  }

  /***
    * Generates a constraint:node for a service
    */
  private def generateConstraint: ServiceTransformer[BenchFlowBenchmark] = bb => service => {
    bb.getAliasForService(service.name) match {
      case Some(alias) => service.copy(environment = Some(service.environment.get :+ s"constraint:node==$alias"))
      case None =>  throw new Exception(s"Server alias not found for service ${service.name}")
    }
  }

  /***
    * Generates constraint:node for all services in a DockerCompose
    */
  private def generateConstraints: DCTransformer[BenchFlowBenchmark] = bb => dc => {
    dc.copy(services = dc.services.map(generateConstraint(bb)))
  }

  /***
    * Given a collector name, returns the corresponding service
    */
  private def resolveCollector(name: String): Service = {
    import scala.io.Source._
    val yaml = fromFile(benv.getBenchFlowServicesPath + s"/$name.collector.yml").mkString
    Service.fromYaml(yaml)
  }

  /***
    * Given a service, returns a list of bound benchflow services
    */
  private def resolveBoundServices(service: Service) = {
    val bindings = bb.getBindingsForService(service.name).map(_.boundService)
    bindings.map(resolveCollector)
  }

  /***
    *  Generates constraint for a bound benchflow service
    */
  private def generateBenchFlowServiceConstraint: BenchFlowServiceTransformer[Service] =
      boundservice => bb => bfservice => {
        val alias = bb.getAliasForService(boundservice.name).get
        bfservice.copy(
           name = s"${bfservice.name}_collector_${boundservice.name}",
           environment = Some(bfservice.environment.get :+ s"constraint:node==$alias"))
      }

  private def generateBenchFlowServiceConstraints(bound: Service, bfservices: Seq[Service]): Seq[Service] =
    bfservices.map(bfservice => generateBenchFlowServiceConstraint(bound)(bb)(bfservice))

  /***
    * Resolves bound benchflow services
    */
  private def resolveBenchFlowServices: DCTransformer[BenchFlowBenchmark] = bb => dc => {
    val bindings = dc.services.map(service => (service, resolveBoundServices(service)))
    val bfservices: Seq[Service] = bindings.flatMap(binding => generateBenchFlowServiceConstraints(binding._1, binding._2))
    dc.copy(services = dc.services ++ bfservices)
  }

  /***
    * resolves all benchflow variables in a docker compose
    */
  private def resolveBenchFlowVariables: DCTransformer[BenchFlowBenchmark] = bb => dc => {

    /** *
      * Given the name of a bound collector, returns the service it is bound to
      */
    def getBoundService(bfservice: Service): Service = {
      val boundTo = bfservice.name.split("_")(2)
      dc.services.filter(s => s.name == boundTo).head
    }

    /***
      * Resolves benchflow variables in a service
      */
    def resolveBenchFlowVariablesForService: ServiceTransformer[Unit] = Unit => service => {

      def resolveBenchFlowVariablesForEntry(entry: String): String = {

        implicit val env = benv

        def updateEntry(resolved: (String, String))(entry: String) =
          entry.replace(s"$${${resolved._1}}", resolved._2)

        val values = entry.findBenchFlowVars.getOrElse(Seq()).map(bfvar => (bfvar, bfvar match {
          case BenchFlowEnvVariable.prefix(prefix, name) => BenchFlowEnvVariable(name).resolve
          case BenchFlowBoundServiceVariable.prefix(prefix, name) => {
            implicit val boundTo = getBoundService(service)
            BenchFlowBoundServiceVariable(name).resolve
          }
          case other => s"$${$bfvar}"
        }))

        values.map(value => updateEntry(value)_).reduceOption(_ compose _) match {
          case None => entry
          case Some(cumulativeUpdate) => cumulativeUpdate(entry)
        }

      }

      service.copy(environment = Some(Environment(
        service.environment.getOrElse(Environment(Seq()))
               .environment.map(resolveBenchFlowVariablesForEntry))))
    }

    dc.copy(services = dc.services.map(resolveBenchFlowVariablesForService()))
  }

  /***
    * Generates a DockerCompose with bound benchflow services and resolved variables
    */
  def build: DockerCompose = {
    val dc = DockerCompose.fromYaml(dcyaml)
    val transformations = List(resolveBenchFlowVariables(bb), resolveBenchFlowServices(bb), generateConstraints(bb))
    val transform = transformations.reduce(_ compose _)
    transform(dc)
  }

}
