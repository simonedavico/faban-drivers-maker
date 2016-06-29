package cloud.benchflow.benchmark.config.benchflowbenchmark

import net.jcazevedo.moultingyaml._

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 23/05/16.
  */
object BenchFlowBenchmarkYamlProtocol extends DefaultYamlProtocol {

  implicit val deployFormat = yamlFormat1(Deploy)
  implicit val targetServiceFormat = yamlFormat2(TargetService)
  implicit val totalTrialsFormat = yamlFormat1(TotalTrials)
  implicit val virtualUsersFormat = yamlFormat1(VirtualUsers)
  implicit val executionFormat = yamlFormat3(Execution)

  implicit object SutsTypeYamlFormat extends YamlFormat[SutsType] {
    override def write(obj: SutsType): YamlValue = ???

    override def read(yaml: YamlValue): SutsType = {
      SutsType(yaml.asYamlObject.getFields(
        YamlString("suts_type")
      ).head.convertTo[String])
    }

  }

  implicit object SutYamlFormat extends YamlFormat[Sut] {
    override def write(sut: Sut): YamlValue = {
      YamlObject(
        YamlString("name") -> YamlString(sut.name),
        YamlString("version") -> YamlString(sut.version.toString),
        YamlString("type") -> YamlString(sut.sutsType match {
          case WfMS => "WfMS"
          case Http => "http"
        })
      )
    }

    override def read(yaml: YamlValue): Sut = {
      val sutName = yaml.asYamlObject.fields.get(YamlString("name")) match {
        case Some(YamlString(name)) => name
        case _ => throw new DeserializationException("No name specified in sut definition")
      }

      val version = yaml.asYamlObject.fields.get(YamlString("version")) match {
        case Some(YamlString(v)) => Version(v)
        case _ => throw new DeserializationException("No version specified in sut definition")
      }

      val sutsType = yaml.asYamlObject.fields.get(YamlString("type")) match {
        case Some(YamlString(t)) => SutsType(t)
        case _ => throw new DeserializationException("No type specified in sut definition")
      }

      Sut(sutName, version, sutsType)
    }
  }

  implicit object PropertiesYamlFormat extends YamlFormat[Properties] {
    override def write(props: Properties): YamlValue = {

      def convertSecond(s: Any): YamlValue = {
        s match {
          case str: String => YamlString(str)
          //TODO: extend with more numeric types, or try to generify
          case int: Int => YamlNumber(int)
          case lst: List[String] => lst.toYaml
          case map: Map[String, Any] => convert(map)
        }
      }

      def convertOne(t: (String, Any)): (YamlValue, YamlValue) = {
        YamlString(t._1) -> convertSecond(t._2)
      }

      def convert(p: Map[String, Any]) = {
        YamlObject(p.map(convertOne))
      }

      convert(props.properties)
    }

    private def toScalaPair(pair: (YamlValue, YamlValue)): (String, Any) = {
      val first = pair._1.convertTo[String]

      def convertValue(value: YamlValue): Any = value match {
        case YamlString(s) => s
        case YamlBoolean(bool) => bool
        case YamlDate(date) => date
        case YamlNumber(num) => num
        case YamlObject(map) => map.seq.map(toScalaPair)
        case YamlArray(values) => values.map(convertValue)//values.toList.map(value => value.convertTo[String])
        case _ => throw DeserializationException("Unexpected format for field properties")
      }

      (first, convertValue(pair._2))

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
    override def write(binding: Binding): YamlValue = {

      binding.config match {
        case Some(config) => YamlObject(
          YamlString(binding.boundService) -> config.toYaml
        )
        case None => YamlString(binding.boundService)
      }

    }

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

    override def write(httpOp: HttpOperation): YamlValue = {
      YamlObject(
        YamlString(httpOp.name) -> {
          YamlObject(
            YamlString("endpoint") -> YamlString(httpOp.endpoint),
            YamlString("method") -> YamlString(httpOp.method.toString),
            YamlString("headers") -> httpOp.headers.toYaml,
            //TODO: check that serializing data as null when absent is ok
            YamlString("data") -> httpOp.data.toYaml
          )
        }
      )
    }


    override def read(yaml: YamlValue): HttpOperation = {

      val fields = yaml.asYamlObject.fields
      val operationName = fields.seq.head._1.convertTo[String]
      val operationBody = fields.seq.head._2.asYamlObject
      val method = HttpMethod(operationBody.getFields(YamlString("method")).map(_.convertTo[String]).head)
      val headersMap = operationBody.getFields(YamlString("headers")).headOption match {
        case None => Map[String, String]()
        case Some(YamlObject(headers)) => YamlObject(headers).convertTo[Map[String, String]]
        case _ => throw new DeserializationException("Invalid format for headers in operation " + operationName)
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
    override def write(wfmsOp: WfMSOperation): YamlValue = {
      wfmsOp.data match {
        case Some(data) => YamlObject(
          YamlString(wfmsOp.name) -> YamlObject(YamlString("data") -> YamlString(data))
        )
        case None => YamlString(wfmsOp.name)
      }
    }

    override def read(yaml: YamlValue): WfMSOperation = {

      yaml match {
        case YamlString(model) => WfMSOperation(name = model, data = None)
        case _ =>
          val fields = yaml.asYamlObject.fields
          val operationName = fields.seq.head._1.convertTo[String]
          val operationBody = fields.seq.headOption.map(_._2.asYamlObject)
          val data = operationBody.flatMap(_.getFields(YamlString("data")).headOption.map(_.convertTo[String]))
          WfMSOperation(name = operationName, data = data)
      }
    }
  }

  implicit object MatrixMixRowYamlFormat extends YamlFormat[MatrixMixRow] {
    override def write(matrixMixRow: MatrixMixRow): YamlValue = matrixMixRow.row.toYaml

    override def read(yaml: YamlValue): MatrixMixRow = {
      yaml.asInstanceOf[YamlArray] match {
        case YamlArray(elems) => MatrixMixRow(elems.map(_.convertTo[Double]))
        case _ => throw new DeserializationException("Incorrect format for matrix mix row")
      }
    }
  }

  implicit object FlatSequenceMixRowYamlFormat extends YamlFormat[FlatSequenceMixRow] {
    override def write(flatSeqMixRow: FlatSequenceMixRow): YamlValue = flatSeqMixRow.row.toYaml

    override def read(yaml: YamlValue): FlatSequenceMixRow = {
      yaml.asInstanceOf[YamlArray] match {
        case YamlArray(elems) => FlatSequenceMixRow(elems.map(_.convertTo[String]))
        case _ => throw new DeserializationException("Incorrect format for flatSequence mix row")
      }
    }
  }

  implicit object FlatSequenceMixYamlFormat extends YamlFormat[FlatSequenceMix] {
    override def write(flatSeqMix: FlatSequenceMix): YamlValue = {
      YamlObject(
        YamlString("flatSequence") ->
          YamlObject(
            YamlString("sequences") -> flatSeqMix.rows.toYaml,
            YamlString("flat") -> flatSeqMix.opsMix.toYaml
          ),
        YamlString("deviation") -> flatSeqMix.deviation.toYaml
      )
    }

    override def read(yaml: YamlValue): FlatSequenceMix = {

      val flatSequenceBody = yaml.asYamlObject.fields.get(YamlString("flatSequence")).get
      val flat = flatSequenceBody.asYamlObject.getFields(
        YamlString("flat")
      ).head match {
        case YamlArray(flatProbs) => flatProbs.map(_.convertTo[Double])
        case _ => throw new DeserializationException("Missing or incorrect format for sequence mix flat probabilities field")
      }

      val sequences = flatSequenceBody.asYamlObject.getFields(
        YamlString("sequences")
      ).head match {
        case YamlArray(rows) => rows.map(_.convertTo[FlatSequenceMixRow])
        case _ => throw new DeserializationException("Missing or incorrect format for sequence mix sequences field")
      }

      val deviation = yaml.asYamlObject.getFields(
        YamlString("deviation")
      ).headOption match {
        case Some(YamlNumber(dev: Double)) => Some(dev)
        case _ => None
      }

      FlatSequenceMix(deviation = deviation, rows = sequences, opsMix = flat)
    }
  }

  implicit object MatrixMixYamlFormat extends YamlFormat[MatrixMix] {

    override def write(matrixMix: MatrixMix): YamlValue = {
      YamlObject(
        YamlString("matrix") -> matrixMix.rows.toYaml,
        YamlString("deviation") -> matrixMix.deviation.toYaml
      )
    }

    override def read(yaml: YamlValue): MatrixMix = {
      val matrixRows = yaml.asYamlObject.fields.get(YamlString("matrix")).get match {
        case YamlArray(rows) => rows.map(_.convertTo[MatrixMixRow])
        case _ => throw new DeserializationException("Incorrect format for matrix mix")
      }

      val deviation = yaml.asYamlObject.getFields(
        YamlString("deviation")
      ).headOption match {
        case Some(YamlNumber(dev: Double)) => Some(dev)
        case _ => None
      }

      MatrixMix(matrixRows, deviation)
    }
  }

  implicit object FlatMixYamlFormat extends YamlFormat[FlatMix] {

    override def write(flatMix: FlatMix): YamlValue = {
      YamlObject(
        YamlString("flat") -> flatMix.opsMix.toYaml,
        YamlString("deviation") -> flatMix.deviation.toYaml
      )
    }

    override def read(yaml: YamlValue): FlatMix = {

      val deviation = yaml.asYamlObject.getFields(
        YamlString("deviation")
      ).headOption match {
        case Some(YamlNumber(dev: Double)) => Some(dev)
        case _ => None
      }

      yaml.asYamlObject.getFields(
        YamlString("flat")
      ).head match {
        case YamlArray(probs) => FlatMix(probs.map(_.convertTo[Double]), deviation)
        case _ => throw new DeserializationException("Unexpected format for flat mix")
      }

    }
  }

  implicit object SequenceMixYamlFormat extends YamlFormat[FixedSequenceMix] {
    override def write(fixedSeq: FixedSequenceMix): YamlValue = {
      YamlObject(
        YamlString("fixedSequence") -> fixedSeq.sequence.toYaml,
        YamlString("deviation") -> fixedSeq.deviation.toYaml
      )
    }

    override def read(yaml: YamlValue): FixedSequenceMix = {

      val deviation = yaml.asYamlObject.getFields(
        YamlString("deviation")
      ).headOption match {
        case Some(YamlNumber(dev: Double)) => Some(dev)
        case _ => None
      }

      yaml.asYamlObject.getFields(
        YamlString("fixedSequence")
      ).head match {
        case YamlArray(sequence) => FixedSequenceMix(sequence.map(_.convertTo[String]), deviation)
        case _ => throw new DeserializationException("Unexpected format for sequence mix")
      }

    }
  }

  implicit object MixYamlFormat extends YamlFormat[Mix] {
    override def read(yaml: YamlValue): Mix = ???

    override def write(mix: Mix): YamlValue = {
      mix match {
        case matrix: MatrixMix => MatrixMixYamlFormat.write(matrix)
        case flat: FlatMix => FlatMixYamlFormat.write(flat)
        case flatSeq: FlatSequenceMix => FlatSequenceMixYamlFormat.write(flatSeq)
        case fixed: FixedSequenceMix =>  SequenceMixYamlFormat.write(fixed)
      }
    }
  }

  implicit object DriverConfigurationYamlFormat extends YamlFormat[DriverConfiguration] {
    override def write(driverConfig: DriverConfiguration): YamlValue = {
      YamlObject(
        YamlString("mix") -> driverConfig.mix.toYaml,
        YamlString("max90th") -> driverConfig.max90th.toYaml,
        YamlString("popularity") -> driverConfig.popularity.toYaml
      )
    }

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

      val max90th = yaml.asYamlObject.fields.get(YamlString("max90th")).map(_.convertTo[Double])
      val popularity = yaml.asYamlObject.fields.get(YamlString("popularity")).map(_.convertTo[String].init.toFloat/100)
      val mix = yaml.asYamlObject.fields.get(YamlString("mix")).map(generateMix)

      DriverConfiguration(
        mix = mix,
        max90th = max90th,
        popularity = popularity
      )
    }
  }


  //TODO: figure out how to make drivers yaml format generic (may not be possible)
  implicit object HttpDriverYamlFormat extends YamlFormat[HttpDriver] {
    override def write(httpDriver: HttpDriver): YamlValue = {
      YamlObject(
        YamlString("http") ->
          YamlObject(
            YamlString("operations") -> httpDriver.operations.toYaml,
            YamlString("configuration") -> httpDriver.configuration.toYaml,
            YamlString("properties") -> httpDriver.properties.toYaml
          )
      )
    }

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
        case _ => throw new DeserializationException("invalid format; drivers section of benchflow-benchmark.yml has to be a list")
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

    override def write(wfmsDriver: WfMSDriver): YamlValue = {

      val driverName = wfmsDriver match {
        case WfMSStartDriver(_,_,_) => "start"
      }

      YamlObject(
        YamlString(driverName) ->
          YamlObject(
            YamlString("operations") -> wfmsDriver.operations.toYaml,
            YamlString("configuration") -> wfmsDriver.configuration.toYaml,
            YamlString("properties") -> wfmsDriver.properties.toYaml
          )
      )
    }

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
        case _ => throw new DeserializationException("invalid format; drivers section of benchflow-benchmark.yml has to be a list")
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

    override def write(bFlowConfig: BenchFlowConfig): YamlValue = {
      bFlowConfig.benchflow_config.toYaml
    }

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

    override def write(sutConfig: SutConfiguration): YamlValue = {
      YamlObject(
        YamlString("target-service") -> sutConfig.targetService.toYaml,
        YamlString("deploy") -> sutConfig.deploy.toYaml,
        YamlString("benchflow-config") -> sutConfig.bfConfig.toYaml
      )
    }

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

    override def write(bb: BenchFlowBenchmark): YamlValue = {
      YamlObject(
        YamlString("sut") -> bb.sut.toYaml,
        YamlString("benchmark_name") -> bb.name.toYaml,
        YamlString("description") -> bb.description.toYaml,
        YamlString("trials") -> bb.trials.trials.toYaml,
        YamlString("virtualUsers") -> bb.virtualUsers.virtualUsers.toYaml,
        YamlString("execution") -> bb.execution.toYaml,
        YamlString("properties") -> bb.properties.toYaml,
        YamlString("drivers") -> {
          bb.sut.sutsType match {
            case WfMS => bb.drivers.map(_.asInstanceOf[WfMSDriver]).toYaml
            case Http => bb.drivers.map(_.asInstanceOf[HttpDriver]).toYaml
          }
        },
        YamlString("sut-configuration") -> bb.sutConfiguration.toYaml
      )
    }

    override def read(yaml: YamlValue): BenchFlowBenchmark = {

      def getObject(key: String)(implicit obj: Map[YamlValue, YamlValue]) =
        YamlObject(YamlString(key) -> obj.get(YamlString(key)).get)

      implicit val bfBmark = yaml.asYamlObject.fields.toMap
      val sut = yaml.asYamlObject.fields.get(YamlString("sut")).get.convertTo[Sut]

      //TODO: figure out if it's possible to avoid matching again on sut type here
      val drivers = sut.sutsType match {
        case WfMS => bfBmark.get(YamlString("drivers")).get.asInstanceOf[YamlArray].elements.map(d => d.convertTo[WfMSDriver])
        case Http => bfBmark.get(YamlString("drivers")).get.asInstanceOf[YamlArray].elements.map(d => d.convertTo[HttpDriver])
        case _ => throw new DeserializationException("Illegal value for suts_type field.")
      }

      val name = bfBmark.get(YamlString("benchmark_name")).get.convertTo[String]
      val description = bfBmark.get(YamlString("description")).get.convertTo[String]
      val properties = getObject("properties").convertTo[Properties]
      val sutConfig = getObject("sut-configuration").convertTo[SutConfiguration]
      val trials = getObject("trials").convertTo[TotalTrials]
      val virtualUsers = getObject("virtualUsers").convertTo[VirtualUsers]
      val execution = bfBmark.get(YamlString("execution")).get.convertTo[Execution]

      BenchFlowBenchmark(
        name = name,
        description = description,
        sut = sut,
        drivers = drivers,
        properties = properties,
        trials = trials,
        sutConfiguration = sutConfig,
        virtualUsers = virtualUsers,
        execution = execution
      )
    }
  }

}
