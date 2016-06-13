package cloud.benchflow.benchmark.heuristics

import cloud.benchflow.benchmark.config.benchflowbenchmark.{Driver, BenchFlowBenchmark}
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv

import scala.reflect.ClassTag

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

  class BaseScaleBalancer(val configuration: Map[String, Any])(private val bb: BenchFlowBenchmark) extends ScaleBalancer {

    val users = bb.virtualUsers.virtualUsers

    private def popularity(driver: Driver[_]): Float =
      driver.configuration.flatMap(_.popularity).getOrElse(1.toFloat/bb.drivers.size)

    def scale(driver: Driver[_]): Int = (users * popularity(driver)).toInt

    private lazy val s = {
      //scale = max(users/pop_d1, users/pop_d2, users/pop_d3...)
      bb.drivers.map(d => users/popularity(d)).max.toInt
    }

    def scale: Int = s

    def threadPerScale(driver: Driver[_]): Float = scale(driver)/scale

  }

  trait ExtendedScaleBalancer extends ScaleBalancer {

    private def threshold = configuration.get("threshold").get.asInstanceOf[Int]

    private def scalingFactor = super.scale/threshold

    abstract override def scale = super.scale/scalingFactor

    abstract override def threadPerScale(driver: Driver[_]) = super.threadPerScale(driver) * scalingFactor

  }

  trait FixedScaleBalancer extends ScaleBalancer {

    abstract override def scale = users

    abstract override def threadPerScale(driver: Driver[_]) = {
      configuration.get("threadPerScale").get.asInstanceOf[Float]
    }

  }

  object ScaleBalancer {

    def apply(strategy: String, configuration: Map[String, Any]) = (bb: BenchFlowBenchmark) => {
      strategy match {
        case "balance" => new BaseScaleBalancer(configuration)(bb) with ExtendedScaleBalancer
        case "simple" => new BaseScaleBalancer(configuration)(bb) with FixedScaleBalancer
        case _ => throw new Exception("Unknown strategy for ScaleBalancer. Implement the strategy into " +
                                      "the ScaleBalancer factory.")
      }
    }

  }
}