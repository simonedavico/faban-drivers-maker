package cloud.benchflow.benchmark.heuristics.threadstart

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.benchmark.heuristics.HeuristicConfiguration
import cloud.benchflow.benchmark.heuristics.scale.ScaleBalancer
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  *         Created on 30/05/16.
  */
class ComputeDelayConfiguration(mapConfig: Map[String, Any]) extends HeuristicConfiguration(mapConfig) {

  val simultaneous = mapConfig.get("simultaneous").get.asInstanceOf[Boolean]
  val parallel = mapConfig.get("parallel").get.asInstanceOf[Boolean]
  val scaleBalancer = mapConfig.get("scaleBalancer").get.asInstanceOf[BenchFlowBenchmark => ScaleBalancer]

}

class ComputeDelayHeuristic(mapConfig: Map[String, Any])(implicit env: BenchFlowEnv)
  extends ThreadStartHeuristic[ComputeDelayConfiguration](mapConfig)(env) {

  override def delay(bb: BenchFlowBenchmark, numOfUsedHosts: Int): Int = {

    //TODO: round half to even?
    val rampUp = bb.execution.rampUp
    val scale = config.scaleBalancer(bb).scale

    (rampUp, config.parallel) match {
      case (0, _) => 0
      // rampUp/scale * 1000
      case (_, false) => rampUp/scale * 1000
      // (rampUp/scale) * #(agents+master utilised) * 1000
      case _ => rampUp/scale * numOfUsedHosts * 1000
    }

  }

  override def simultaneous(bb: BenchFlowBenchmark): Boolean = config.simultaneous

  override def parallel(bb: BenchFlowBenchmark): Boolean = config.parallel
}
