package cloud.benchflow.config

import cloud.benchflow.config.benchflowbenchmark.{Binding, BenchFlowBenchmark}
import cloud.benchflow.config.docker.compose.DockerCompose
import cloud.benchflow.driversmaker.utils.BenchFlowEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 13/02/16.
  */
class BenchFlowConfigurationBuilder(val bfEnv: BenchFlowEnv,
                                    dockercompose: String,
                                    benchFlowBenchmark: String) {

  var benchflowBenchmark = BenchFlowBenchmark.fromYaml(benchFlowBenchmark)
  var dockerCompose = DockerCompose.fromYaml(dockercompose)

  def build() = {
      val resolvedServices = dockerCompose.services.map(generateForService)
      val benchFlowServices = resolvedServices.flatMap(resolveBoundBenchFlowServices)
      dockerCompose = dockerCompose.copy(services = resolvedServices ++ benchFlowServices)
  }

  /***
    *
    * Given an environment and a constraint, adds the constraint to the environment
    */
  private def resolveNodeConstraint(env: Environment, constraint: String): Environment = {
//    val ip = bfEnv.getVariable[String](s"BENCHFLOW_SERVER_${constraint.toUpperCase}_PUBLICIP")
    val resolved: String = "constraint:node==" + constraint
    env :+ resolved
  }

  /***
    *
    * returns a service rapresentation for a collector, given its name
    *
    */
  private def resolveBenchFlowCollector(collectorName: String): Service = {
    val collector = scala.io.Source
                            .fromFile(bfEnv.getBenchFlowServicesPath + s"/$collectorName.collector.yml")
                            .mkString
    Service.fromYaml(collector)
  }

  /***
    *
    * resolves all the bindings for a given service
    */
  private def resolveBoundBenchFlowServices(service: Service): Seq[Service] = {
    val toBind = benchflowBenchmark.`sut-configuration`.bfConfig.bindings(service.name)

    def getServiceForBinding(boundTo: Service)(binding: Binding) = {
      var bfService = resolveBenchFlowCollector(binding.boundService)
      bfService = bfService.copy(name = s"${bfService.name}_${boundTo.name}")

      val serverAlias = benchflowBenchmark.`sut-configuration`.deploy.get(service.name) match {
        case None => throw new Exception(s"Service ${service.name} doesn't feature a deploy alias")
        case Some(alias) => alias
      }

      bfService.copy(environment =
        Some(resolveNodeConstraint(bfService.environment.getOrElse(Environment(List())),
                                   serverAlias)))
    }

    toBind match {
      case None => Seq()
      case Some(bindings) => bindings.map(getServiceForBinding(service))
    }
  }

  /***
    *
    * Generates the necessary fields for a service
    */
  private def generateForService(service: Service): Service = {

    val serverAlias = benchflowBenchmark.`sut-configuration`.deploy.get(service.name) match {
      case None => throw new Exception(s"cannot generate constraint for service ${service.name}")
      case Some(alias) => alias
    }

    service.copy(
      environment = Some(resolveNodeConstraint(
                          service.environment.getOrElse(Environment(List())
                         ), serverAlias)))
  }

}
