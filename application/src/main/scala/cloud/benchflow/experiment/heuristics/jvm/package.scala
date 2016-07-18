package cloud.benchflow.experiment.heuristics

import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv
import cloud.benchflow.test.config.experiment.BenchFlowExperiment

import scala.reflect.ClassTag

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 30/05/16.
  */
package object jvm {

  //configure xmx and xms params
  abstract class JvmParamsHeuristic[A <: HeuristicConfiguration : ClassTag](mapConfig: Map[String, Any])(env: BenchFlowEnv)
    extends Heuristic[A](mapConfig)(env) {

    def xmx(bb: BenchFlowExperiment): Int

    def xms(bb: BenchFlowExperiment): Int

  }

  object JvmParamsHeuristic {
    def apply(strategy: String, configuration: Map[String, Any])(implicit env: BenchFlowEnv) = strategy match {
      case "simple" => new SimpleJvmParamsHeuristic(configuration)(env)
      case "logistic" => new LogisticsGrowthJvmParamsHeuristic(configuration)(env)
    }
  }

}
