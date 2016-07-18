package cloud.benchflow.experiment.heuristics

import cloud.benchflow.experiment.config.experimentdescriptor.BenchFlowExperiment
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv

import scala.reflect.ClassTag

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 30/05/16.
  */
package object threadstart {

  abstract class ThreadStartHeuristic[A <: HeuristicConfiguration : ClassTag](config: Map[String, Any])(env: BenchFlowEnv)
    extends Heuristic[A](config)(env) {

    def delay(bb: BenchFlowExperiment, numOfUsedHosts: Int): Int
    def simultaneous(bb: BenchFlowExperiment): Boolean
    def parallel(bb: BenchFlowExperiment): Boolean

  }
  object ThreadStartHeuristic {

    def apply(strategy: String, configuration: Map[String, Any])(implicit env: BenchFlowEnv): ThreadStartHeuristic[_] = strategy match {
      case "computeDelay" => new ComputeDelayHeuristic(configuration)
    }
  }

}
