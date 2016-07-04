package cloud.benchflow.config

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 16/02/16.
 */
object DesignTest extends App {

  //  case class EnvVar(val variable: String)

  abstract class BenchFlowVariable(val variable: String) {
    type Source
    def resolve(implicit source: Source): (String, String)
  }

  case class BenchFlowEnvVar(override val variable: String) extends BenchFlowVariable(variable) {
    type Source = List[(String, String)]
    def resolve(implicit source: Source) = source.head
  }

  case class BenchFlowConfigVar(override val variable: String) extends BenchFlowVariable(variable) {
    type Source = Map[String, String]
    def resolve(implicit source: Source) = (variable, source.get(variable).get)
  }

  object MyFoo {
    implicit val source1 = Map("a" -> "b", "c" -> "d", "configvar" -> "boh")
    implicit val source2 = List(("a", "b"), ("c", "d"))

    def doStuff(variable: String) = variable match {
      case "envvar" ⇒ BenchFlowEnvVar("envvar").resolve
      case "configvar" ⇒ BenchFlowConfigVar("configvar").resolve
    }

  }

  println(MyFoo.doStuff("envvar"))
  println(MyFoo.doStuff("configvar"))

  //function composition
  type Adder = Int ⇒ Int
  val a: Adder = n ⇒ n + 1
  val b: Adder = n ⇒ n * 2
  val funcs: List[Adder] = List(a, b)
  val newFunc = funcs.reduce(_ compose _)

  println(newFunc(1))

  abstract class Foo
  case class Foo1() extends Foo
  case class Foo2(val bubu: String, val foo1: Foo1) extends Foo

  //  def handleFoo(foo1: Foo1) = println("foo1")
  //  def handleFoo(foo2: Foo2) = println("foo2")

  def handleFoo[T <: Foo](foo: T) = foo match {
    case Foo1() ⇒ println("foo1")
    case Foo2(_, Foo1()) ⇒ println("foo2")
  }

  val foo1 = new Foo1
  val foo2 = new Foo2("", foo1)

  val list: List[Foo] = List(foo1, foo2)

  for (foo ← list) handleFoo(foo)

}
