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
import org.openmole.core.workflow.composition.DSL.tasks
import org.openmole.core.workflow.mole.Mole
import org.openmole.core.workflow.task.FromContextTask
import org.openmole.core.workflow.validation._
import org.openmole.plugin.domain.collection._
import org.scalatest._
import org.openmole.plugin.domain.bounds._
import org.openmole.plugin.method.evolution.Genome.GenomeBound

class WorkflowSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.test.Stubs._

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
          genome = Seq(x in (0.0, 1.0), y in (0.0, 1.0)),
          objective = Seq(x, y),
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
          genome = Seq(population in (0.0, 1.0), state in (0.0, 1.0)),
          objective =
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
      populationSize = 200,
      genome = Seq(xArray in Vector.fill(5)((0.0, 1.0)), yArray in Vector.fill(5)((0, 1))),
      objective = Seq()
    )
  }

  import org.openmole.core.workflow.test.Stubs._

  "Evolution" should "run" in {
    @volatile var executed = 0

    val a = Val[Double]

    val testTask =
      FromContextTask("test") { p ⇒
        import p._
        executed += 1
        context
      } set ((inputs, outputs) += a)

    val nsga = NSGA2Evolution(
      evaluation = testTask,
      objective = Seq(a),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      parallelism = 10
    )

    nsga run

    executed should be >= 100
  }

  "Island evolution" should "run" in {
    @volatile var executed = 0

    val a = Val[Double]

    val testTask =
      FromContextTask("test") { p ⇒
        import p._
        executed += 1
        context
      } set ((inputs, outputs) += a)

    val nsga = NSGA2Evolution(
      evaluation = testTask,
      objective = Seq(a),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100
    ) by Island(5)

    nsga run

    executed should be >= 100
  }

  "Hook" should "be valid" in {
    @volatile var executed = 0

    val a = Val[Double]

    val testTask =
      FromContextTask("test") { p ⇒
        import p._
        executed += 1
        context
      } set ((inputs, outputs) += a)

    val nsga =
      NSGA2Evolution(
        evaluation = testTask,
        objective = Seq(a),
        genome = Seq(a in (0.0, 1.0)),
        termination = 100
      ) hook ("/tmp/test.txt")

    Validation(nsga) match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }

    Validation(nsga by Island(10)) match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }
  }

  "Steady state workflow" should "have no validation error" in {
    val mole: Mole = nsga2()
    Validation(mole).toList match {
      case Nil ⇒
      case l   ⇒ sys.error(s"Several validation errors have been found in ${mole}: " + l.mkString("\n"))
    }

    Validation(conflict).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }
  }

  "Island workflow" should "have no validation error" in {
    val islandEvolutionNSGA2 = IslandEvolution(nsga2(), 10, 50, 100)

    Validation(islandEvolutionNSGA2).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }

    Validation(IslandEvolution(conflict, 10, 50, 100)).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }
  }

  "Steady state workflow with wrapping" should "have no validation error" in {
    Validation(nsga2(wrap = false)).toList match {
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

  "NSGAEvolution" should "be valid" in {
    val a = Val[Double]
    val b = Val[Double]

    val nsga = NSGA2Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100
    )

    Validation(nsga).isEmpty should equal(true)
  }

  "NSGAEvolution" should "be possible to generate" in {
    val a = Val[Double]
    val b = Val[Double]

    def nsga(i: Int) =
      NSGA2Evolution(
        evaluation = EmptyTask() set (inputs += a, outputs += b),
        objective = Seq(b),
        genome = Seq(a in (0.0, 1.0)),
        termination = 100
      )

    val wf = EmptyTask() -- (0 until 2).map(nsga)
    Validation(wf).isEmpty should equal(true)
  }

  "Passing an input from previous task" should "be valid" in {
    val a1 = Val[Double]
    val a2 = Val[Double]
    val b = Val[Double]

    val preTask =
      EmptyTask() set (
        outputs += a2
      )

    val nsga2 =
      NSGA2Evolution(
        evaluation = EmptyTask() set (inputs += (a1, a2), outputs += b),
        objective = Seq(b),
        genome = Seq(a1 in (0.0, 1.0)),
        termination = 100,
        distribution = Island(1)
      )

    val wf = preTask -- nsga2

    Validation(wf).isEmpty should equal(true)
  }

  "NSGAEvolution with island" should "be valid" in {
    val a = Val[Double]
    val b = Val[Double]

    val nsga = NSGA2Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100
    )

    Validation(nsga).isEmpty should equal(true)
  }

  "NSGAEvolution with delta" should "be valid" in {
    val a = Val[Double]
    val b = Val[Double]

    val wf = NSGA2Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b delta 1.0),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100
    )

    Validation(wf).isEmpty should equal(true)
  }

  "NSGAEvolution with maximisation" should "be valid" in {
    val a = Val[Double]
    val b = Val[Double]

    val wf = NSGA2Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(-b),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100
    )

    Validation(wf).isEmpty should equal(true)
  }

  "Stochastic NSGAEvolution" should "be valid" in {
    val a = Val[Double]
    val b = Val[Double]

    val nsga = NSGA2Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      stochastic = Stochastic()
    ) by Island(1)

    Validation(nsga).isEmpty should equal(true)
  }

  "Stochastic NSGAEvolution with aggregate and delta" should "be valid" in {
    val a = Val[Double]
    val b = Val[Double]

    val nsga = NSGA2Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b aggregate median delta 100),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      stochastic = Stochastic()
    )

    Validation(nsga).isEmpty should equal(true)
  }

  "Stochastic NSGAEvolution with island" should "be valid" in {
    val a = Val[Double]
    val b = Val[Double]

    val nsga = NSGA2Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      stochastic = Stochastic(),
      distribution = Island(1)
    )

    Validation(nsga).isEmpty should equal(true)
  }

  "Suggestion" should "be possible" in {
    val a = Val[Double]

    NSGA2Evolution(
      evaluation = EmptyTask(),
      objective = Seq(a),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      suggestion = Seq(Seq(a := 0.5))
    )
  }

  "Aggregation" should "be possible in NSGA" in {

    val a = Val[Double]
    val b = Val[Double]

    def f(v: Vector[Double]) = v.head

    val nsga = NSGA2Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b aggregate f _ as "aggF"),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      stochastic = Stochastic()
    )

    Validation(nsga).isEmpty should equal(true)
  }

  "Aggregation" should "be possible in PSE" in {
    val a = Val[Double]
    val b = Val[Double]
    def f(v: Vector[Double]) = v.head

    PSEEvolution(
      evaluation = EmptyTask(),
      objective = Seq(a aggregate f _ in (0.0 to 1.0 by 0.1), b in (0.2 to 0.5 by 0.1)),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      stochastic = Stochastic()
    )
  }

  "Aggregation" should "be possible in OSE" in {
    val o = Val[Double]
    val a = Val[Double]
    val b = Val[Double]
    def f(v: Vector[Double]) = v.head

    OSEEvolution(
      origin = Seq(o in (0.0 to 1.0 by 0.1)),
      evaluation = EmptyTask(),
      objective = Seq(a aggregate f _ under 9, b under 3.0),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      stochastic = Stochastic()
    )
  }

  "OMRHook" should "work with NSGA" in {
    import org.openmole.plugin.hook.omr._

    val a = Val[Double]
    val b = Val[Double]

    val nsga =
      NSGA2Evolution(
        evaluation = EmptyTask() set (inputs += a, outputs += b),
        objective = Seq(b),
        genome = Seq(a in (0.0, 1.0)),
        termination = 100,
        stochastic = Stochastic()
      )

    nsga hook ("/tmp/test", format = OMROutputFormat())
  }

  "By" should "generate island" in {
    val a = Val[Double]
    val b = Val[Double]

    val nsga =
      NSGA2Evolution(
        evaluation = EmptyTask() set (inputs += a, outputs += b),
        objective = Seq(b),
        genome = Seq(a in (0.0, 1.0)),
        termination = 100
      )

    val wf: DSLContainer[EvolutionWorkflow] = nsga hook ("/tmp/test")
    val wf2: DSLContainer[EvolutionWorkflow] = nsga hook ("/tmp/test") by Island(100)

    // FIXME improve this test when more metadata are added to EvolutioWorkflow
    tasks(wf.dsl).flatMap(_.task.name).exists(_.contains("island")) should equal(false)
    tasks(wf2.dsl).flatMap(_.task.name).exists(_.contains("island")) should equal(true)
  }

  "by and hook" should "be supported by all evolution methods" in {
    val a = Val[Double]
    val b = Val[Double]

    val p1: DSL = NSGA2Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100
    ) by Island(10) hook ("/tmp/test")

    val p2: DSL = NSGA3Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100
    ) by Island(10) hook ("/tmp/test")

    val p3: DSL = OSEEvolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b under 1.0),
      origin = Seq(a in (0.0 to 1.0 by 0.1)),
      termination = 100
    ) by Island(10) hook ("/tmp/test")

    val p4: DSL = ProfileEvolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b),
      genome = Seq(a in (0.0, 1.0)),
      profile = Seq(a),
      termination = 100
    ) by Island(10) hook ("/tmp/test")

    val p5: DSL = PSEEvolution(
      evaluation = EmptyTask() set (inputs += a, outputs += b),
      objective = Seq(b in (0.0 to 1.0 by 0.1)),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100
    ) by Island(10) hook ("/tmp/test")
  }
  "by Island" should "be usable with by" in {
    val a = Val[Double]
    val b = Val[Double]

    val p: DSL =
      NSGA2Evolution(
        evaluation = EmptyTask() set (inputs += a, outputs += b),
        objective = Seq(b),
        genome = Seq(a in (0.0, 1.0)),
        termination = 100
      ) on LocalEnvironment(1) by Island(10) by 10
  }

}