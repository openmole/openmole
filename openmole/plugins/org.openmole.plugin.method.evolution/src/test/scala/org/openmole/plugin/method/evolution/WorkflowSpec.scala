/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.workflow.puzzle.{ PuzzleContainer, ToPuzzle }
import org.openmole.core.workflow.tools.DefaultSet
import org.openmole.core.workflow.validation.Validation
import org.openmole.plugin.domain.collection._
import org.scalatest._
import org.openmole.tool.types._
import org.openmole.plugin.domain.bounds._
import org.openmole.core.workflow.tools.Stubs._
import org.openmole.plugin.method.evolution.Genome.GenomeBound

class WorkflowSpec extends FlatSpec with Matchers {

  def nsga2(wrap: Boolean = true) = {
    val x = Val[Double]
    val y = Val[Double]
    val z = Val[Int]

    val puzzle = EmptyTask() set (
      (inputs, outputs) += (x, y, z),
      z := 9
    )

    // Define a builder to use NSGA2 generational EA algorithm.
    // replicateModel is the fitness function to optimise.
    // lambda is the size of the offspring (and the parallelism level).
    SteadyStateEvolution(
      algorithm =
        NSGA2(
          genome = Seq(x in (0.0, 1.0), y in ("0.0", "1.0")),
          objectives = Seq(x, y),
          stochastic = Stochastic()
        ),
      evaluation = puzzle,
      parallelism = 10,
      termination = 10,
      wrap = wrap
    )
  }

  def conflict = {
    val population = Val[Double]
    val state = Val[Double]

    val puzzle = EmptyTask() set (
      (inputs, outputs) += (population, state)
    )

    // Define a builder to use NSGA2 generational EA algorithm.
    // replicateModel is the fitness function to optimise.
    // lambda is the size of the offspring (and the parallelism level).
    SteadyStateEvolution(
      algorithm =
        PSE(
          genome = Seq(population in (0.0, 1.0), state in ("0.0", "1.0")),
          objectives =
            Seq(
              population in (0.0 to 1.0 by 0.1),
              state in (0.0 to 1.0 by 0.1)
            ),
          stochastic = Stochastic()
        ),
      evaluation = puzzle,
      parallelism = 10,
      termination = 10
    )
  }

  def boundArray = {
    val xArray = Val[Array[Double]]
    val yArray = Val[Array[Int]]

    NSGA2(
      mu = 200,
      genome = Seq(xArray in Vector.fill(5)((0.0, 1.0)), yArray in Vector.fill(5)(("0", "1"))),
      objectives = Seq()
    )
  }

  import org.openmole.core.workflow.tools.Stubs._

  "Bound array" should "compile" in {
    SteadyStateEvolution(
      algorithm = boundArray,
      evaluation = EmptyTask(),
      parallelism = 10,
      termination = 10
    )
  }

  "Island evolution" should "compile" in {
    val steady = SteadyStateEvolution(
      algorithm = boundArray,
      evaluation = EmptyTask(),
      parallelism = 10,
      termination = 10
    )
    IslandEvolution(steady, 10, 10)
  }

  "Steady state workflow" should "have no validation error" in {
    Validation(nsga2().head.toMole).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }

    Validation(conflict.head.toMole).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }
  }

  "Island workflow" should "have no validation error" in {
    val islandEvolutionNSGA2 = IslandEvolution(nsga2(), 10, 50, 100).head.toMole

    Validation(islandEvolutionNSGA2).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }

    Validation(IslandEvolution(conflict, 10, 50, 100).head.toMole).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }
  }

  "Steady state workflow with wrapping" should "have no validation error" in {
    Validation(nsga2(wrap = false).head.toMole).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }
  }

  "Genome bounds" should "compile" in {
    import org.openmole.plugin.domain.collection._
    val ba = Val[Array[Boolean]]
    val b1: GenomeBound = ba in Seq(Vector(true, false), Vector(true, false))
    val b2: GenomeBound = ba in 2
  }

  "Suggestion" should "be possible" in {

    val a = Val[Double]

    NSGA2Evolution(
      evaluation = EmptyTask(),
      objectives = Seq(a),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      suggested =
        Seq(
          Seq(a := 0.5)
        )

    )
  }

}