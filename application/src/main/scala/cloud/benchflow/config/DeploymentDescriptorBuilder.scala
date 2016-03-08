package cloud.benchflow.config

import cloud.benchflow.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.config.docker.compose.DockerCompose
import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.driversmaker.utils.env.DriversMakerBenchFlowEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 16/02/16.
  */
class DeploymentDescriptorBuilder(val bb: BenchFlowBenchmark, val benv: DriversMakerBenchFlowEnv) {

  type DCTransformer = DockerCompose => DockerCompose
  type ServiceTransformer = Service => Service
  type BenchFlowServiceTransformer = Service => ServiceTransformer

  abstract class BenchFlowVariable(val name: String) {
    type Source
    def resolve(implicit source: Source): String
  }

  case class BenchFlowEnvVariable(override val name: String) extends BenchFlowVariable(name) {
    type Source = DriversMakerBenchFlowEnv
    override def resolve(implicit source: Source): String = source.getVariable[String](s"BENCHFLOW_$name")
  }
  object BenchFlowEnvVariable {
    val prefix = "(BENCHFLOW_ENV_)(.*)".r
  }

  case class BenchFlowBoundServiceVariable(override val name: String) extends BenchFlowVariable(name) {
    override type Source = Service

    override def resolve(implicit source: Source): String = {
      name match {
        case "IP" => getLocalIp(bb.getAliasForService(source.name).get)
        case "PORT" => source.getPort.getOrElse("BENCHFLOW_BENCHMARK_BOUNDSERVICE_PORT")
        case "CONTAINER_NAME" => source.containerName.map(_.container_name)
                                       .getOrElse("BENCHFLOW_BENCHMARK_BOUNDSERVICE_CONTAINER_NAME")
        case other => s"BENCHFLOW_BENCHMARK_BOUNDSERVICE_$other"
      }
    }
  }
  object BenchFlowBoundServiceVariable {
    val prefix = "(BENCHFLOW_BENCHMARK_BOUNDSERVICE_)(.*)".r
  }

  case class BenchFlowConfigVariable(override val name: String) extends BenchFlowVariable(name) {
    override type Source = (Service, Service)

    override def resolve(implicit source: Source): String = {
      val bfservice = source._2
      val boundTo = source._1
      bb.getBindingConfiguration(boundTo.name, bfservice.name.split("_")(0)) match {
        case Some(config) =>
          config.properties.get(name).get.toString
        case None => name
      }
    }
  }
  object BenchFlowConfigVariable {
    val prefix = "(BENCHFLOW_BENCHMARK_CONFIG_)(.*)".r
  }

  /***
    * Returns the local ip for a given alias
    */
  private def getLocalIp(alias: String) = benv.getVariable[String](s"BENCHFLOW_SERVER_${alias.toUpperCase}_PRIVATEIP")

  private def getPublicIp(alias: String) = benv.getVariable[String](s"BENCHFLOW_SERVER_${alias.toUpperCase}_PUBLICIP")

  /***
    * Generates a constraint:node for a service
    */
  private def generateConstraint: ServiceTransformer = service => {
    bb.getAliasForService(service.name) match {
      case Some(alias) => service.copy(environment = Some(service.environment.get :+ s"constraint:node==$alias"),
        ports = Some(Ports(Seq(getLocalIp(alias) + ":" + service.ports.get.ports.head))))
      case None =>  throw new Exception(s"Server alias not found for service ${service.name}")
    }
  }

  /***
    * Generates constraint:node for all services in a DockerCompose
    */
  private def generateConstraints: DCTransformer = dc => {
    dc.copy(services = dc.services.map(generateConstraint))
  }

  /***
    * Given a collector name, returns the corresponding service
    */
  private def resolveCollector(name: String): Service = {
    import scala.io.Source.fromFile
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
  private def generateBenchFlowServiceConstraint: BenchFlowServiceTransformer =
      boundservice => bfservice => {
        val alias = bb.getAliasForService(boundservice.name).get
        bfservice.copy(
           name = s"${bfservice.name}_collector_${boundservice.name}",
           environment = Some(bfservice.environment.get :+ s"constraint:node==$alias"),
           ports = Some(Ports(Seq(getLocalIp(alias) + ":" + bfservice.ports.get.ports.head))))
      }

  private def generateBenchFlowServiceConstraints(bound: Service, bfservices: Seq[Service]): Seq[Service] =
    bfservices.map(bfservice => generateBenchFlowServiceConstraint(bound)(bfservice))

  /***
    * Resolves bound benchflow services
    */
  private def resolveBenchFlowServices: DCTransformer = dc => {
    val bindings = dc.services.map(service => (service, resolveBoundServices(service)))
    val bfservices: Seq[Service] = bindings.flatMap(b => generateBenchFlowServiceConstraints(b._1, b._2))
    dc.copy(services = dc.services ++ bfservices)
  }

  /***
    * resolves all benchflow variables in a docker compose
    */
  private def resolveBenchFlowVariables: DCTransformer = dc => {

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
    def resolveBenchFlowVariablesForService: ServiceTransformer = service => {

      def resolveBenchFlowVariablesForEntry(entry: String): String = {

        implicit val env = benv

        def updateEntry(resolved: (String, String))(entry: String) =
          entry.replace(s"$${${resolved._1}}", resolved._2)

        val values = entry.findBenchFlowVars.getOrElse(Seq()).map(bfvar => (bfvar, bfvar match {
          case BenchFlowEnvVariable.prefix(prefix, name) => BenchFlowEnvVariable(name).resolve
          case BenchFlowBoundServiceVariable.prefix(prefix, name) =>
            implicit val boundTo = getBoundService(service)
            BenchFlowBoundServiceVariable(name).resolve
          case BenchFlowConfigVariable.prefix(prefix, name) =>
            implicit val source = (getBoundService(service), service)
            BenchFlowConfigVariable(name).resolve
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

    dc.copy(services = dc.services.map(resolveBenchFlowVariablesForService))
  }

  /***
    * Adds trial related informations to a service
    */
  def addTrialInfoForService: Trial => ServiceTransformer = trial => service => {

    val expId = s"BENCHFLOW_EXPERIMENT_ID=${trial.getExperimentId}"
    val trialId = s"BENCHFLOW_TRIAL_ID=${trial.getTrialId}"
    val total = s"BENCHFLOW_TRIAL_TOTAL=${trial.getTotalTrials}"
    val cName = service.name + "_" + trial.getTrialId

    service.copy(containerName = Some(ContainerName(cName)),
                 environment = Some(service.environment.get :+ expId :+ trialId :+ total :+
                                                            s"BENCHFLOW_CONTAINER_NAME=$cName"))
  }

  /***
    * Adds trial related informations to a docker compose
    */
  def addTrialInfo: Trial => DCTransformer = trial => dc => {
    dc.copy(services = dc.services.map(addTrialInfoForService(trial)))
  }

  /***
    * Generates a DockerCompose with bound benchflow services and resolved variables
    */
  def build(dc: DockerCompose, trial: Trial): DockerCompose = {
    val transformations = List(addTrialInfo(trial), resolveBenchFlowVariables,
                               resolveBenchFlowServices, generateConstraints)
    val transform = transformations.reduce(_ compose _)
    transform(dc)
  }

}
