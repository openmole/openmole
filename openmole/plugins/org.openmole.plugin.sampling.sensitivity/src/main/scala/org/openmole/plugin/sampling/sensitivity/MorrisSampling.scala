/*
 * Copyright (C) 2018 Samuel Thiriot
 *                    Romain Reuillon
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
 * Implements the sensitivity analysis *screening* method proposed by Morris in 1991.
 * Attempts to match the principles of his proposal, without the exact process
 * which heavily relies on matrices computation. We notably:
 * - ensure a different independent seed is used for each trajectory
 * - ensure the order of exploration of variables is uniform and independent for each trajectory
 * - ensure the direction of exploration of variables is uniform and independant for each trajectory
 *
 * The sampling method produces lists of elements to explore in the form:
 *
 * For factors
 *   [x1,    x2,   x3,  x4]
 * A trajectory is in the form:
 *   values for factors,         factor   delta   trajectoryid  iterationid
 * [ [12.1,  0.5,  1.2,  5.3]    ""        0      12            0
 *   [12.1,  0.8,  1.2,  5.3]    "x2"      +0.3   12            1
 *   [12.1,  0.8,  1.2,  7.1]    "x4"     +1.8    12            2
 *   [8.1,   0.8,  1.2,  7.1]    "x1"     -4.0    12            3
 *   [8.1,   0.8   1.0,  7.1]    "x3"     -0.2    12            4
 * ]
 * As a consequence, when processing the results, one can find all the iterations in which variable "x2" was changed, by
 * which delta, and what the reference case is for it (iterationid-1)
 *
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
import FromContext.asMonad._

/**
 * A trajectory in the Morris OAT understanding,
 * that is a set of points in an N-dimensional spaces,
 * with a seed being the starting point, then each next
 * point changing only one variable of a given delta.
 * A raw trajectory is expressed with indices of factors
 * and a space of parameters in [0:1]
 */
case class Trajectory(
  seed:          Array[Double],
  variableOrder: List[Int],
  deltas:        List[Double],
  points:        List[Array[Double]]
)

object MorrisSampling {

  def apply(
    repetitions: FromContext[Int],
    levels:      FromContext[Int],
    verbose:     Boolean                          = false,
    factors:     Seq[ScalarOrSequenceOfDouble[_]]) =
    new MorrisSampling(repetitions, levels, verbose, factors)

  /**
   * Namespace for the variables peculiar to this Morris sampling
   */
  val namespace = Namespace("morrissampling")

  /**
   * The variable named like this contains the name of the factor which was changed
   * in a given point.
   */
  val varFactorName = Val[String]("factorname", namespace = namespace)
  val varDelta = Val[Double]("delta", namespace = namespace)

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
   * order=(1,2,0) and directions (+1,+1,+1), it returns a list of hyperpoints (hyperpoint = one value per factor),
   * along with a list stating what factor was changed, and another stating how much change was induced.
   * List(
   *   Array(0.5, 0.5, 0.5)     factoridx=1     delta=-0.25
   *   Array(0.5, 0.5, 0.75)    factoridx=2     delta=+0.25
   *   Array(0.75, 0.5, 0.75)   factoridx=0     delta=+0.25
   *   )
   */
  @annotation.tailrec
  def trajectoryBuilder(delta: Double, order: List[Int], direction: List[Int], seed: Array[Double],
                        accPoints: List[Array[Double]] = List(),
                        accDeltas: List[Double]        = List()): (List[Array[Double]], List[Double]) = {

    if (order.isEmpty) (accPoints.reverse, accDeltas.reverse)
    else {
      val idx: Int = order.head
      val deltaOriented: Double = delta * direction.head
      val newSeed: Array[Double] = seed.updated(idx, seed(idx) + deltaOriented)
      //System.out.println("changing " + idx + "th element of " + deltaOriented)
      trajectoryBuilder(delta, order.tail, direction.tail, newSeed,
        newSeed :: accPoints, deltaOriented :: accDeltas)
    }
  }

  /**
   * Builds a trajectory for k variables sampled with p levels, builds a k+1 long trajectory.
   * For instance for k=3, p=4, it might return: TrajectoryRaw(
   *   seed = Array(0.25, 0.5, 0.75),
   *   variablesOrder = (2,1,0),
   *   deltas = (-0.25,+0.25,+0.25),
   *   points = List(
   *        Array(0.25, 0.5, 1.0)
   *        Array(0.25, 0.75, 1.0)
   *        Array(0.0, 0.75, 1.0))
   * )
   */
  def trajectory(k: Int, p: Int, rng: scala.util.Random): Trajectory = {
    val delta: Double = 1.0 / p.toDouble
    val seed: Array[Double] = MorrisSampling.seed(k, p, rng)
    val orderVariables: List[Int] = rng.shuffle(0 to k - 1).toList
    val directionVariables: List[Int] = Array.fill(k)(rng.nextInt(2) * 2 - 1).toList
    val (points, deltas) = trajectoryBuilder(delta, orderVariables, directionVariables, seed)
    Trajectory(seed, orderVariables, deltas, points)
  }

  /**
   * Generates r independent trajectories for k variables sampled with p levels.
   */
  @annotation.tailrec
  def trajectories(k: Int, p: Int, r: Int, rng: scala.util.Random, acc: List[Trajectory] = List()): List[Trajectory] = {
    if (r == 0) return acc
    else trajectories(k, p, r - 1, rng, trajectory(k, p, rng) :: acc)
  }

}

sealed class MorrisSampling(
  val repetitions: FromContext[Int],
  val levels:      FromContext[Int],
  val verbose:     Boolean                          = false,
  val factors:     Seq[ScalarOrSequenceOfDouble[_]]) extends Sampling {

  override def inputs = factors.flatMap(_.inputs)
  override def prototypes = factors.map { _.prototype } ++ Seq(
    MorrisSampling.varDelta,
    MorrisSampling.varFactorName)

  /**
   * Converts a raw trajectory to a list of lists of variables (points to run)
   * to execute in a given context. Replaces the index of factor by the variable name,
   * and scales each of the points in a n-dimensional [0:1] space into their actual range.
   */
  def trajectoryToVariables(t: Trajectory, idTraj: Int): FromContext[List[List[Variable[_]]]] = FromContext { p ⇒

    import p._
    // forge the list of variables for the first run (reference run)
    val variablesForRefRun: List[Variable[_]] = List(
      Variable(MorrisSampling.varFactorName, ""),
      Variable(MorrisSampling.varDelta, 0.0)
    ) ++ ScalarOrSequenceOfDouble.scaled(factors, t.seed).from(context)

    // forge lists of lists of variables for the runs of the trajectory
    val variablesForElementaryEffects = (t.points, t.deltas, t.variableOrder.zipWithIndex).zipped.map(
      (point: Array[Double], delta: Double, order2idx: (Int, Int)) ⇒ {
        val factoridx = order2idx._1
        val iterationId = order2idx._2 + 1
        List(
          Variable(MorrisSampling.varFactorName, factors(factoridx).prototype.name),
          Variable(MorrisSampling.varDelta, point(factoridx) - t.seed(factoridx))
        ) ++ ScalarOrSequenceOfDouble.scaled(factors, point).from(context)
      })

    variablesForRefRun :: variablesForElementaryEffects

  }

  override def apply(): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { ctxt ⇒
    import ctxt._
    val r: Int = repetitions.from(context)
    val p: Int = levels.from(context)
    val k: Int = factors.length

    val trajectoriesRaw: List[Trajectory] = MorrisSampling.trajectories(k, p, r, random())

    if (verbose) {
      System.out.println("should explore a " + k + "-dimensions " +
        p + "-levels grid with " +
        r + " repetitions => " +
        trajectoriesRaw.length + " trajectories")
    }

    trajectoriesRaw.zipWithIndex.flatMap {
      case (t, idTraj) ⇒
        trajectoryToVariables(t, idTraj).from(context)
    }.toIterator
  }

}
