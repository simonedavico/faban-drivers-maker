package cloud.benchflow.experiment.heuristics.scale

import cloud.benchflow.test.config.Driver


/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 27/06/16.
  */
trait ExtendedScaleBalancer extends ScaleBalancer {

  private def threshold = configuration.get("threshold").get.asInstanceOf[Int]

  private def scalingFactor = super.scale/threshold

  abstract override def scale = super.scale/scalingFactor

  abstract override def threadPerScale(driver: Driver[_]) = super.threadPerScale(driver) * scalingFactor

}