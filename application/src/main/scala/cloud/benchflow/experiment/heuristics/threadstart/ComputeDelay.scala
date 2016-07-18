package cloud.benchflow.experiment.heuristics.threadstart

import cloud.benchflow.experiment.config.experimentdescriptor.BenchFlowExperiment
import cloud.benchflow.experiment.heuristics.HeuristicConfiguration
import cloud.benchflow.experiment.heuristics.scale.ScaleBalancer
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  *         Created on 30/05/16.
  */
class ComputeDelayConfiguration(mapConfig: Map[String, Any]) extends HeuristicConfiguration(mapConfig) {

  val simultaneous = mapConfig.get("simultaneous").get.asInstanceOf[Boolean]
  val parallel = mapConfig.get("parallel").get.asInstanceOf[Boolean]
  val scaleBalancer = mapConfig.get("scaleBalancer").get.asInstanceOf[BenchFlowExperiment => ScaleBalancer]

}

class ComputeDelayHeuristic(mapConfig: Map[String, Any])(implicit env: BenchFlowEnv)
  extends ThreadStartHeuristic[ComputeDelayConfiguration](mapConfig)(env) {

  override def delay(bb: BenchFlowExperiment, numOfUsedHosts: Int): Int = {

    val rampUp = bb.execution.rampUp
    val scale = config.scaleBalancer(bb).scale

    val toRound = (rampUp, config.parallel) match {
      case (0, _) => 0
      // rampUp/scale * 1000
      case (_, false) => rampUp.toFloat/scale * 1000
      // (rampUp/scale) * #(agents+master utilised) * 1000
      case _ => rampUp.toFloat/scale * numOfUsedHosts * 1000
    }

    //round result half to even
    (toRound*2).toInt/2

  }

  override def simultaneous(bb: BenchFlowExperiment): Boolean = config.simultaneous

  override def parallel(bb: BenchFlowExperiment): Boolean = config.parallel
}
