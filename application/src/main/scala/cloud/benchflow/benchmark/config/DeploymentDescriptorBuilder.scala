package cloud.benchflow.benchmark.config

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.benchmark.config.docker.compose.DockerCompose
import cloud.benchflow.driversmaker.requests.Trial
import cloud.benchflow.driversmaker.utils.env.DriversMakerEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 16/02/16.
  */
class DeploymentDescriptorBuilder(val bb: BenchFlowBenchmark,
                                  val benv: DriversMakerEnv) {

  type DCTransformer = DockerCompose => DockerCompose
  type ServiceTransformer = Service => Service
  type BenchFlowServiceTransformer = Service => ServiceTransformer

  abstract class BenchFlowVariable(val name: String) {
    type Source
    def resolve(implicit source: Source): String
  }

  case class BenchFlowEnvVariable(override val name: String) extends BenchFlowVariable(name) {
    type Source = DriversMakerEnv
    override def resolve(implicit source: Source): String = source.getConfigYml.getVariable[String](s"BENCHFLOW_$name")
  }
  object BenchFlowEnvVariable {
    val prefix = "(BENCHFLOW_ENV_)(.*)".r
  }

  case class BenchFlowBoundServiceVariable(override val name: String) extends BenchFlowVariable(name) {
    override type Source = Service

    override def resolve(implicit source: Source): String = {
      name match {
        case "IP" => getLocalIp(bb.getAliasForService(source.name).get)
        case "PORT" => source.getPorts.getOrElse("BENCHFLOW_BENCHMARK_BOUNDSERVICE_PORT")
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
//      bb.getBindingConfiguration(boundTo.name, bfservice.name.split("_")(0)) match {
      bb.getBindingConfiguration(boundTo.name, bfservice.name.split("\\.")(2)) match {
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
  private def getLocalIp(alias: String) =
    benv.getConfigYml.getVariable[String](s"BENCHFLOW_SERVER_${alias.toUpperCase}_PRIVATEIP")

  private def getPublicIp(alias: String) =
    benv.getConfigYml.getVariable[String](s"BENCHFLOW_SERVER_${alias.toUpperCase}_PUBLICIP")

  private def getIp(alias: String) = {
    getPublicIp(alias)
//    val localIp = getLocalIp(alias)
//    if (localIp == null) getPublicIp(alias)
//    else localIp
  }

  private def isBenchFlowService(service: Service) =
    service.name.contains("benchflow.collector") || service.name.contains("benchflow.monitor")

  /***
    * Adds fields needed by BenchFlow to a service
    */
  private def generateBenchFlowFieldsForService: ServiceTransformer = service => {
    bb.getAliasForService(service.name) match {
      case Some(alias) =>
        service.copy(
          environment = service.environment.map(_ :+ s"constraint:node==$alias"),
          ports = Some(Ports(Seq(getIp(alias) + ":" + service.ports.get.ports.head)))
        )
      case None =>  throw new Exception(s"Server alias not found for service ${service.name}")
    }
  }

  /***
    * Generates fields needed by BenchFlow for all services in a DockerCompose
    */
  private def generateBenchFlowFields: DCTransformer = dc => {
    dc.copy(services = dc.services.map(generateBenchFlowFieldsForService))
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
    *  Generates fields required by BenchFlow for a bound benchflow service
    */
  private def generateFieldsForBenchFlowService: BenchFlowServiceTransformer =
      boundservice => bfservice => {
        val alias = bb.getAliasForService(boundservice.name).get
        //val name = s"${bfservice.name}_collector_${boundservice.name}"
        val name = s"benchflow.collector.${bfservice.name}.${boundservice.name}"
        bfservice.copy(
           name = name,
           environment = Some(bfservice.environment.get :+
                              s"constraint:node==$alias" :+
                              s"BENCHFLOW_COLLECTOR_NAME=$name" :+
                              s"ENVCONSUL_CONSUL=${benv.getEnvConsulAddress}"),
           ports = Some(Ports(Seq(getIp(alias) + ":" + bfservice.ports.get.ports.head))))
      }

  private def generateFieldsForBenchFlowServices(bound: Service, bfservices: Seq[Service]): Seq[Service] =
    bfservices.map(bfservice => generateFieldsForBenchFlowService(bound)(bfservice))

  /***
    * Resolves bound benchflow services
    */
  private def resolveBenchFlowServices: DCTransformer = dc => {
    val bindings = dc.services.map(service => (service, resolveBoundServices(service)))
    val bfservices: Seq[Service] = bindings.flatMap(b => generateFieldsForBenchFlowServices(b._1, b._2))
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
//      val boundTo = bfservice.name.split("_")(2)
      val boundTo = bfservice.name.split("\\.")(3)
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

        //since each entry could have more than one benchflow variable, this mapping
        //cumulatively resolves them
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

    val newEnv = if(isBenchFlowService(service)) {
      service.environment.map(env =>
        env :+
        expId :+
        trialId :+
        total :+
        s"BENCHFLOW_CONTAINER_NAME=$cName" :+
        s"SUT_NAME=${bb.sut.name}" :+
        s"SUT_VERSION=${bb.sut.version}"
      )
    } else service.environment

    service.copy(
      containerName = Some(ContainerName(cName)),
      environment = newEnv
    )
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
    val transformations = List(resolveBenchFlowVariables, addTrialInfo(trial),
                               resolveBenchFlowServices, generateBenchFlowFields)
    val transform = transformations.reduce(_ compose _)
    transform(dc)
  }

}
