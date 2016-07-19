package cloud.benchflow.experiment.heuristics

import cloud.benchflow.test.config.Driver
import cloud.benchflow.test.config.experiment.BenchFlowExperiment

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 30/05/16.
  */
package object scale {

  trait ScaleBalancer {
    protected val configuration: Map[String, Any]

    def users: Int
    def scale: Int
    def scale(driver: Driver[_]): Int
    def threadPerScale(driver: Driver[_]): Float
  }

  object ScaleBalancer {

    def apply(strategy: String, configuration: Map[String, Any]) = (bb: BenchFlowExperiment) => {
      strategy match {
        case "balance" => new BaseScaleBalancer(configuration)(bb) with ExtendedScaleBalancer
        case "simple" => new BaseScaleBalancer(configuration)(bb) with FixedScaleBalancer
        case _ => throw new Exception("Unknown strategy for ScaleBalancer. Implement the strategy into " +
                                      "the ScaleBalancer factory.")
      }
    }

  }

}