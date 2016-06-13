package cloud.benchflow.benchmark.heuristics.scale

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.benchmark.heuristics.HeuristicConfiguration
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 30/05/16.
  */
class SimpleScaleConfiguration(config: Map[String, Any]) extends HeuristicConfiguration(config) {

  val threadsPerScale = config.get("threadsPerScale").get.asInstanceOf[Float]

}

class SimpleScaleHeuristic(mapConfig: Map[String, Any])(implicit env: BenchFlowEnv)
  extends ScaleHeuristic[SimpleScaleConfiguration](mapConfig)(env) {

  override def scale(bb: BenchFlowBenchmark): Double = bb.virtualUsers.virtualUsers

  override def threadsPerScale(bb: BenchFlowBenchmark): Float = config.threadsPerScale

}