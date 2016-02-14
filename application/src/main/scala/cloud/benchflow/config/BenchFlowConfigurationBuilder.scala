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
      dockerCompose = resolveBenchFlowVariables(dockerCompose.copy(services = resolvedServices ++ benchFlowServices))
  }


  private def resolveBenchFlowVariables(dc: DockerCompose): DockerCompose = {

    def updateEntry(entry: String, resolvedValue: (String, String)): String = {
      entry.replace(s"$${${resolvedValue._1}}", resolvedValue._2)
    }

    //resolves all the benchflow variables in a service
    def resolveService(s: Service) = {

      def benchflowEnvResolver(variable: String, dc: DockerCompose) =
        bfEnv.getVariable[String](s"BENCHFLOW_$variable")

      //given a variable, it returns the value
      def resolveSingleVariable(variable: String) = {
        val benchflow_env = "(BENCHFLOW_ENV_)(.*)".r
        variable match {
          case benchflow_env(prefix, name) => benchflowEnvResolver(name, dc)
          case other =>  s"$${$other}"
        }
      }

      //given an environment entry, returns the entry with benchflow variables resolved
      def resolveEnvironmentEntry(entry: String) = {
        val resolvedValues = entry.findBenchFlowVars
                                  .getOrElse(Seq())
                                  .map(v => (v, resolveSingleVariable(v)))

        var resolvedString = entry
        for(resolvedValue <- resolvedValues) {
          resolvedString = updateEntry(resolvedString, resolvedValue)
        }
        resolvedString
      }

      s.copy(environment = Some(Environment(s.environment.get.environment.map(resolveEnvironmentEntry))))
    }

    DockerCompose(dc.services.map(resolveService))

  }

  /***
    *
    * Given an environment and a constraint, adds the constraint to the environment
    */
  private def resolveNodeConstraint(env: Environment, constraint: String): Environment = {
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
