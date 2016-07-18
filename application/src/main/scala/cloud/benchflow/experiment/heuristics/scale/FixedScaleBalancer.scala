package cloud.benchflow.experiment.heuristics.scale

import cloud.benchflow.experiment.config.experimentdescriptor.Driver

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 27/06/16.
  */
trait FixedScaleBalancer extends ScaleBalancer {

  abstract override def scale = users

  abstract override def threadPerScale(driver: Driver[_]) = {
    configuration.get("threadPerScale").get.asInstanceOf[Float]
  }

}
