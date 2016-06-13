package cloud.benchflow.benchmark.heuristics.scale

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.benchmark.heuristics.HeuristicConfiguration
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 30/05/16.
  */
class BalancedScaleConfiguration(config: Map[String, Any]) extends HeuristicConfiguration(config) {

  val virtualUsersThreshold = config.get("virtualUsersThreshold").get.asInstanceOf[Int]

}

class BalancedScaleHeuristic(mapConfig: Map[String, Any])(implicit env: BenchFlowEnv)
  extends ScaleHeuristic[BalancedScaleConfiguration](mapConfig)(env) {

  override def scale(bb: BenchFlowBenchmark): Double =
    bb.virtualUsers.virtualUsers / threadsPerScale(bb)

  override def threadsPerScale(bb: BenchFlowBenchmark): Float =
    (bb.virtualUsers.virtualUsers / config.virtualUsersThreshold) * 2

}
