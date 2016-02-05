package cloud.benchflow.config.converter

import java.io.FileInputStream

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 26/12/15.
  */
object QuickTest extends App {
  val in = new FileInputStream("./application/src/test/resources/camundaTest.yml")
  print(new BenchFlowConfigConverter("", "") convertAndStringify in)
}
