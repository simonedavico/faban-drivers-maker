package cloud.benchflow.experiment.heuristics.scale

import cloud.benchflow.experiment.config.experimentdescriptor.{BenchFlowExperiment, Driver}

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 27/06/16.
  */
class BaseScaleBalancer(protected val configuration: Map[String, Any])
                       (private val bb: BenchFlowExperiment) extends ScaleBalancer {

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
