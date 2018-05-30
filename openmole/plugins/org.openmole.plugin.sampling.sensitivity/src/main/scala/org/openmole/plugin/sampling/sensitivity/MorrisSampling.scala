/*
 * Copyright (C) 2018 Samuel Thiriot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Morris, M.D. (1991). "Factorial Sampling Plans for Preliminary Computational Experiments"
 *     Technometrics 33: 161–174. doi:10.2307/1269043.
 */

package org.openmole.plugin.sampling.sensitivity

import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.tools.math._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools._
import cats.implicits._

object MorrisSampling {

  def apply(repetitions: FromContext[Int], levels: FromContext[Int], factors: ScalarOrSequenceOfDouble[_]*) =
    new MorrisSampling(repetitions, levels, factors: _*)

  /**
   * For a given count of factors k, a number of levels p,
   * generates an initial seed for the k factors.
   * For instance for k=5 and p=4, it might return Array(0.25, 0.75, 0.25, 0.75, 0.5)
   */
  def seed(k: Int, p: Int, rng: scala.util.Random): Array[Double] = {
    val delta: Double = 1.0 / p.toDouble
    (1 to k).map(_ ⇒ (rng.nextInt(p - 1) + 1).toDouble * delta).toArray
  }

  /**
   * recursively builds trajectories based on a given delta, an initial seed,
   * arbitrary orders and directions for changing variables.
   * For instance, for delta=0.25, seed=(0.5,0.25,0.5),
   * order=(1,2,0) and directions (+1,+1,+1), it returns
   * List(
   *   Array(0.5, 0.5, 0.5)
   *   Array(0.5, 0.5, 0.75)
   *   Array(0.75, 0.5, 0.75)
   *   )
   */
  @annotation.tailrec
  def trajectoryBuilder(delta: Double, order: Array[Int], direction: Array[Int], seed: Array[Double], res: List[Array[Double]]): List[Array[Double]] = {
    if (order.isEmpty) res.reverse
    else {
      val idx: Int = order.head
      val newSeed: Array[Double] = seed.updated(idx, seed(idx) + delta * direction.head)
      System.out.println("changing " + idx + "th element of " + delta * direction.head)
      trajectoryBuilder(delta, order.tail, direction.tail, newSeed, newSeed :: res)
    }
  }

  /**
   * Builds a trajectory for k variables sampled with p levels, builds a k+1 long trajectory.
   * For instance for k=3, p=4, it might return: List(
   *   Array(0.25, 0.5, 0.75)
   *   Array(0.25, 0.5, 1.0)
   *   Array(0.25, 0.75, 1.0)
   *   Array(0.0, 0.75, 1.0))
   */
  def trajectory(k: Int, p: Int, rng: scala.util.Random): List[Array[Double]] = {
    val delta: Double = 1.0 / p.toDouble
    val seed: Array[Double] = MorrisSampling.seed(k, p, rng)
    val orderVariables: Array[Int] = rng.shuffle(0 to k - 1).toArray
    val directionVariables: Array[Int] = Array.fill(k)(rng.nextInt(2) * 2 - 1)
    seed :: trajectoryBuilder(delta, orderVariables, directionVariables, seed, List())
  }

  /**
    * Generates r independent trajectories for k variables sampled with p levels.
    * The result looks like List(
    * List(
    *   Array(0.25, 0.5, 0.75)
    *   Array(0.25, 0.5, 1.0)
    *   Array(0.25, 0.75, 1.0)
    *   Array(0.0, 0.75, 1.0)),
    * ...
    * )
    */
  @annotation.tailrec
  def trajectories(k: Int, p: Int, r: Int, rng: scala.util.Random, acc: List[List[Array[Double]]] = List()): List[List[Array[Double]]] = {
    if (r == 0) return acc
    else trajectories(k, p, r - 1, rng, trajectory(k, p, rng) :: acc)
  }
}

sealed class MorrisSampling(
  val repetitions: FromContext[Int],
  val levels:      FromContext[Int],
  val factors:     ScalarOrSequenceOfDouble[_]*) extends Sampling {

  override def inputs = factors.flatMap(_.inputs)
  override def prototypes = factors.map { _.prototype }

  override def apply() = FromContext { ctxt ⇒
    import ctxt._
    val r: Int = repetitions.from(context)
    val p: Int = levels.from(context)
    val k: Int = factors.length
    System.out.println("should explore a " + k + "-dimensions " + p + "-levels grid with " + r + " repetitions")
    val traj: List[List[Array[Double]]] = MorrisSampling.trajectories(k, p, r, random())
    System.out.println("will work on " + traj.length + " trajectories")
    val flatten: List[Seq[Double]] = traj.flatten.map(a ⇒ a.toSeq)
    System.out.println("leading to " + flatten.length + " runs")
    flatten.map(v ⇒ ScalarOrSequenceOfDouble.scaled(factors, v).from(context)).toIterator
  }

}
