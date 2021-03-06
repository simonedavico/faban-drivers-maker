package cloud.benchflow.benchmark.heuristics

import cloud.benchflow.benchmark.config.benchflowbenchmark.{Driver, BenchFlowBenchmark}
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv

import scala.reflect.ClassTag

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 09/06/16.
  */
package object allocation {

  abstract class AllocationHeuristic[A <: HeuristicConfiguration : ClassTag](config: Map[String, Any])(env: BenchFlowEnv)
    extends Heuristic[A](config)(env) {

    type HostAddress = String
    type Host = (HostAddress, Float)

    protected def agentHostWeight: Float
    protected def masterHostWeight: Float

    private def makeHost(address: String, weight: Float): Host = (address, weight)
    private def makeAgentHost(address: String) = makeHost(address, agentHostWeight)
    private def makeMasterHost(address: String) = makeHost(address, masterHostWeight)

    protected final val agentHosts: List[Host] = {
      import scala.collection.JavaConverters._
      env.getVariable[java.util.List[String]]("BENCHFLOW_FABAN_AGENTS").asScala.toList.map(makeAgentHost)
    }

    protected final val masterHost: Host = {
      val masterAddress = env.getVariable[String]("BENCHFLOW_FABAN_MASTER")
      makeMasterHost(masterAddress)
    }

    //returns a mapping driver -> (host, agents) for a given configuration
    def agents(bb: BenchFlowBenchmark): Map[Driver[_], Set[(HostAddress, Int)]]

  }
  object AllocationHeuristic {

    def apply(strategy: String, configuration: Map[String, Any])(implicit env: BenchFlowEnv) = strategy match {
      case "static" => new StaticAllocationHeuristic(configuration)(env)
    }

  }

}