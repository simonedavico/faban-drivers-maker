package cloud.benchflow.benchmark.heuristics.allocation

import cloud.benchflow.benchmark.config.benchflowbenchmark.{Driver, BenchFlowBenchmark}
import cloud.benchflow.benchmark.heuristics.HeuristicConfiguration
import cloud.benchflow.driversmaker.utils.env.BenchFlowEnv

/**
  * @author Simone D'Avico (simonedavico@gmail.com)
  *
  * Created on 09/06/16.
  */
class StaticAllocationHeuristicConfiguration(config: Map[String, Any])
  extends HeuristicConfiguration(config: Map[String, Any]) {

  import cloud.benchflow.benchmark.heuristics.scale.ScaleBalancer

  val agentHostWeight = config.get("agentHostWeight").get.asInstanceOf[Float]
  val masterHostWeight = config.get("masterHostWeight").get.asInstanceOf[Float]
  val scaleBalancer = config.get("scaleBalancer").get.asInstanceOf[BenchFlowBenchmark => ScaleBalancer]
  val agentAllocationThreshold = config.get("agentAllocationThreshold").get.asInstanceOf[Int]

}

class StaticAllocationHeuristic(mapConfig: Map[String, Any])(implicit env: BenchFlowEnv)
  extends AllocationHeuristic[StaticAllocationHeuristicConfiguration](mapConfig)(env) {

  override protected def agentHostWeight = config.agentHostWeight
  override protected def masterHostWeight = config.masterHostWeight

  //returns a mapping driver -> (host, agents) for a given configuration
  override def agents(bb: BenchFlowBenchmark): Map[Driver[_], Set[(HostAddress, Int)]] = {

    def allocate(driver: Driver[_]): Set[(HostAddress, Int)] = {

      def scale(driver: Driver[_]) = config.scaleBalancer(bb).scale(driver)

      def distribute(numOfAgents: Int, hosts: List[Host]) = {

        val distributedAmounts = scala.collection.mutable.ListBuffer.empty[Int]
        var totalDistribution = hosts.map(_._2).sum
        var remainingAgents = numOfAgents

        for(host <- hosts) {
          val weight = host._2
          val p = weight/totalDistribution
          val distributedAmount = Math.round(p * remainingAgents)
          distributedAmounts += distributedAmount
          totalDistribution -= weight
          remainingAgents -= distributedAmount
        }

        //hosts.zipWithIndex { case (h: Host, index) => (h._1, distributedAmounts(index)) }
        //this is faster
        val hostsWithAgents = for { i <- hosts.indices } yield (hosts(i)._1, distributedAmounts(i))
        hostsWithAgents.toSet
      }

      val numOfAgents = scale(driver) / config.agentAllocationThreshold

      val hosts = numOfAgents < agentHosts.size + 1 match {
        //allocate one agent to each host
        case true => agentHosts.take(numOfAgents).map { case (addr, w) => (addr, 1) }.toSet
        //distribute agents according to host weight
        case false => distribute(numOfAgents, masterHost :: agentHosts)
      }

      hosts
    }

    bb.drivers.map(d => (d, allocate(d))).toMap

  }

}