package cloud.benchflow.experiment.heuristics

import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv
import cloud.benchflow.experiment.heuristics.jvm._
import cloud.benchflow.experiment.heuristics.scale._
import cloud.benchflow.experiment.heuristics.threadstart._
import cloud.benchflow.experiment.heuristics.allocation._

class GenerationDefaults(private implicit val benv: BenchFlowEnv) {

  lazy val jvm = JvmParamsHeuristic(
    strategy = "simple",
    configuration = Map(
      "xms" -> 124,
      "xmx" -> 2048
    )
  )

  lazy val threadStart = ThreadStartHeuristic(
    strategy = "computeDelay",
    configuration = Map(
      "simultaneous" -> false,
      "parallel" -> true,
      "scaleBalancer" -> scaleBalancer
    )
  )

  lazy val scaleBalancer = ScaleBalancer(
    strategy = "balance",
    configuration = Map(
      "threshold" -> 500
    )
  )

  lazy val allocationHeuristic = AllocationHeuristic(
    strategy = "static",
    configuration = Map(
      "agentHostWeight" -> 1f,
      "masterHostWeight" -> 0.5f,
      "scaleBalancer" -> scaleBalancer,
      "agentAllocationThreshold" -> 100
    )
  )

}
object GenerationDefaults {

  val percentiles = Seq("25", "50", "75", "90", "95", "99.9")

  val deviation: Double = 5

  val timeSync = false

  val interval = 1

}
