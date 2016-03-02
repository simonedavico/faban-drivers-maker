package cloud.benchflow.config.benchflowbenchmark

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
case class SutsType(`suts_type`: String)
case class Properties(properties: Map[String, Any])
case class Driver(name: String, properties: Properties)
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
                              drivers: Seq[Driver],
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

  implicit val sutsTypeFormat = yamlFormat1(SutsType)
  implicit val deployFormat = yamlFormat1(Deploy)
  implicit val targetServiceFormat = yamlFormat2(TargetService)
  implicit val totalTrialsFormat = yamlFormat1(TotalTrials)

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

  implicit object DriverYamlFormat extends YamlFormat[Driver] {
    override def write(obj: Driver): YamlValue = ???

    override def read(yaml: YamlValue): Driver = {
      val driver = yaml.asYamlObject.fields.head
      val driverName = driver._1.convertTo[String]
      val properties = YamlObject(YamlString("properties") -> driver._2.asYamlObject).convertTo[Properties]
      Driver(driverName, properties)
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
      val name = bfBmark.get(YamlString("benchmark_name")).get.convertTo[String]
      val description = bfBmark.get(YamlString("description")).get.convertTo[String]
      val properties = getObject("properties").convertTo[Properties]
      val sutConfig = getObject("sut-configuration").convertTo[SutConfiguration]
      val drivers = bfBmark.get(YamlString("drivers")).get.asInstanceOf[YamlArray].elements.map(driver => driver.convertTo[Driver])
      //val trials = bfBmark.get(YamlString("trials")).get.convertTo[Int]
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


