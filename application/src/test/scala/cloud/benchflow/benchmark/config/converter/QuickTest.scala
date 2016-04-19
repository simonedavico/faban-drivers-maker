package cloud.benchflow.benchmark.config.converter

import java.io.FileInputStream

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  *         Created on 05/02/16.
  */
object QuickTest extends App {
  val in = new FileInputStream("./application/src/test/resources/camundaTest.yml")
  print(new BenchFlowBenchmarkConfigConverter("", "") convertAndStringify in)
}
