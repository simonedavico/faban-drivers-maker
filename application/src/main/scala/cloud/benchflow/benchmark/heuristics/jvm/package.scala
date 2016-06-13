package cloud.benchflow.benchmark.heuristics

import cloud.benchflow.benchmark.config.benchflowbenchmark.BenchFlowBenchmark
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv

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

    def xmx(bb: BenchFlowBenchmark): Int

    def xms(bb: BenchFlowBenchmark): Int

  }

  object JvmParamsHeuristic {
    def apply(strategy: String, configuration: Map[String, Any])(implicit env: BenchFlowEnv) = strategy match {
      case "simple" => new SimpleJvmParamsHeuristic(configuration)(env)
      case "logistic" => new LogisticsGrowthJvmParamsHeuristic(configuration)(env)
    }
  }

}
