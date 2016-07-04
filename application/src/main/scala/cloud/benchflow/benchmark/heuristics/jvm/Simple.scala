package cloud.benchflow.benchmark.heuristics.jvm

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.benchmark.heuristics.HeuristicConfiguration
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  *         Created on 30/05/16.
  */
class JvmParamsHeuristicConfiguration(mapConfig: Map[String, Any]) extends HeuristicConfiguration(mapConfig)
{
  val xmx = mapConfig.get("xmx").get.asInstanceOf[Int]
  val xms = mapConfig.get("xms").get.asInstanceOf[Int]
}


class SimpleJvmParamsHeuristic(mapConfig: Map[String, Any])(implicit env: BenchFlowEnv)
  extends JvmParamsHeuristic[JvmParamsHeuristicConfiguration](mapConfig)(env) {

  override def xms(bb: BenchFlowBenchmark) = config.xms
  override def xmx(bb: BenchFlowBenchmark) = config.xmx

}
