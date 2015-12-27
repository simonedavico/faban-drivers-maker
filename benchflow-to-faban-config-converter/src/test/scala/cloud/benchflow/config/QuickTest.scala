package cloud.benchflow.config

import java.io.FileInputStream
import scala.xml.PrettyPrinter

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 26/12/15.
  */
object QuickTest extends App {
  val in = new FileInputStream("./benchflow-to-faban-config-converter/src/test/resources/anotherTest.yaml")
  print(new PrettyPrinter(60, 2) format
    new BenchflowConfigConverter("/usr/lib/jvm/java7", "-Xmx256m -Xms64m -XX:+DisableExplicitGC").from(in))
}
