package cloud.benchflow.config

import java.io.FileInputStream
import scala.xml.PrettyPrinter

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 26/12/15.
  */
object QuickTest extends App {
  val in = new FileInputStream("./benchflow-to-faban-config-converter/src/test/resources/camundaTest.yml")
  print(new BenchFlowConfigConverter("", "") from in)
}
