package cloud.benchflow.experiment.heuristics.jvm

import cloud.benchflow.experiment.heuristics.HeuristicConfiguration
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv
import cloud.benchflow.test.config.experiment.BenchFlowExperiment

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 30/05/16.
  */
class LogisticsGrowthJvmParamsHeuristicConfiguration(mapConfig: Map[String, Any])
  extends HeuristicConfiguration(mapConfig) {

  val k = mapConfig.get("k").get.asInstanceOf[Int]
  val xms = mapConfig.get("xms").get.asInstanceOf[Int]
  val maxUsers = mapConfig.get("maxUsers").get.asInstanceOf[Int]
  val maxMemory = mapConfig.get("maxMemory").get.asInstanceOf[Double]

}


class LogisticsGrowthJvmParamsHeuristic(mapConfig: Map[String, Any])(env: BenchFlowEnv)
  extends JvmParamsHeuristic[LogisticsGrowthJvmParamsHeuristicConfiguration](mapConfig)(env) {

  //see https://en.wikipedia.org/wiki/Logistic_function
  override def xmx(bb: BenchFlowExperiment): Int = {
    val L = config.maxMemory
    val x = bb.users.users
    val x0 = config.maxUsers/2
    (L/(1 + Math.exp(-config.k * (x - x0))) + config.xms).toInt
  }

  override def xms(bb: BenchFlowExperiment): Int = config.xms

}
