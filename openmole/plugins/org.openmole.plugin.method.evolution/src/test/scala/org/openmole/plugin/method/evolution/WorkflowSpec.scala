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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.workflow.composition.DSL.tasks
import org.openmole.core.workflow.mole.Mole
import org.openmole.core.workflow.task.FromContextTask
import org.openmole.core.workflow.validation.*
import org.openmole.plugin.domain.collection.{*, given}
import org.scalatest.*
import org.openmole.plugin.domain.bounds.{*, given}
import org.openmole.plugin.method.evolution.Genome.GenomeBound

object WorkflowSpec:
  enum En:
    case E1, E2

class WorkflowSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test._

  def nsga2 = {
    import EvolutionWorkflow._

    val x = Val[Double]
    val y = Val[Double]
    val z = Val[Int]

    val puzzle = EmptyTask() set (
      (inputs, outputs) += (x, y, z),
      z := 9
    )

    NSGA2Evolution(
      genome = Seq(x in (0.0, 1.0), y in (0.0, 1.0)),
      objective = Seq(x, y),
      stochastic = Stochastic(),
      evaluation = puzzle,
      parallelism = 10,
      termination = 10
    )
  }

  def conflict =
    import EvolutionWorkflow._

    val population = Val[Double]
    val state = Val[Double]

    val puzzle = EmptyTask() set (
      (inputs, outputs) += (population, state)
    )

    PSEEvolution(
      genome = Seq(population in (0.0, 1.0), state in (0.0, 1.0)),
      objective =
        Seq(
          population in (0.0 to 1.0 by 0.1),
          state in (0.0 to 1.0 by 0.1)
        ),
      stochastic = Stochastic(),
      evaluation = puzzle,
      parallelism = 10,
      termination = 10
    )

  "Bounds" should "be accepted for arrays" in {
    val xArray = Val[Array[Double]]
    val yArray = Val[Array[Int]]

    NSGA2Evolution(
      genome = Seq(xArray in Vector.fill(5)((0.0, 1.0)), yArray in Vector.fill(5)((0, 1))),
      objective = Seq(),
      parallelism = 10,
      termination = 10,
      evaluation = EmptyTask()
    )
  }

  import org.openmole.core.workflow.test.Stubs._

  "Evolution" should "run" in {
    @volatile var executed = 0

    val a = Val[Double]
    val ba = Val[Array[Boolean]]

    val testTask =
      FromContextTask("test") { p =>
        import p._
        executed += 1
        assert(context(ba).size == 10)
        context
      } set ((inputs, outputs) += (a, ba))

    val nsga = NSGA2Evolution(
      evaluation = testTask,
      objective = Seq(a),
      genome = Seq(a in (0.0, 1.0), ba in Seq.fill(10)(TrueFalse)),
      termination = 100,
      parallelism = 10
    )

    nsga run

    executed should be >= 100
  }

  "Evolution" should "support single objective" in {
    val a = Val[Double]

    val nsga = NSGA2Evolution(
      evaluation = EmptyTask(),
      objective = a,
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      parallelism = 10
    )
  }


  "Island evolution" should "run" in {
    @volatile var executed = 0

    val a = Val[Double]

    val testTask =
      FromContextTask("test") { p =>
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

  "Serialized Island Evolution" should "run" in {
    val a = Val[Double]

    val testTask =
      FromContextTask("test") { p =>
        import p._
        context
      } set ((inputs, outputs) += a)

    val nsga = NSGA2Evolution(
      evaluation = testTask,
      objective = Seq(a),
      genome = Seq(a in (0.0, 1.0)),
      termination = 10,
      parallelism = 1
    ) by Island(5)

    serializeDeserialize(nsga) run

    val nsga2 = NSGA2Evolution(
      evaluation = testTask,
      objective = Seq(a),
      genome = Seq(a in(0.0, 1.0)),
      termination = 10,
      parallelism = 1,
      suggestion = Suggestion(a := 0.3)
    ) by Island(5)

    serializeDeserialize(nsga2) run
  }

  "Hook" should "be valid" in {
    @volatile var executed = 0

    val a = Val[Double]

    val testTask =
      FromContextTask("test") { p =>
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
      ) hook ("/tmp/test.txt", frequency = 2)

    Validation(nsga) match 
      case Nil =>
      case l   => sys.error("Several validation errors have been found: " + l.mkString("\n"))

    Validation(nsga by Island(10)) match 
      case Nil =>
      case l   => sys.error("Several validation errors have been found: " + l.mkString("\n"))
    
  }

  "Steady state workflow" should "have no validation error" in {
    val mole: Mole = nsga2

    Validation(mole).toList match {
      case Nil =>
      case l   => sys.error(s"Several validation errors have been found in ${mole}: " + l.mkString("\n"))
    }

    Validation(conflict).toList match {
      case Nil =>
      case l   => sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }
  }

  "Island workflow" should "have no validation error" in {
    import EvolutionWorkflow._

    Validation(nsga2 by Island(10)).toList match {
      case Nil =>
      case l   => sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }

    Validation(conflict by Island(10)).toList match {
      case Nil =>
      case l   => sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }
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
      objective = Seq(b evaluate median delta 100),
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
    val b = Val[Int]

    NSGA2Evolution(
      evaluation = EmptyTask(),
      objective = Seq(a),
      genome = Seq(a in (0.0, 1.0), b in (1 to 10)),
      termination = 100,
      suggestion = Seq(Suggestion(a := 0.5, b := 8))
    )
  }

  "Aggregation" should "be possible in NSGA" in {

    val a = Val[Double]
    val b = Val[Double]

    def f(v: Double) = v / 2

    val nsga = NSGA2Evolution(
      evaluation = EmptyTask() set (inputs += a, outputs += (a, b)),
      objective = Seq(b evaluate f as "aggF", a evaluate "a / 2"),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100
    )

    Validation(nsga).isEmpty should equal(true)
  }

  "Aggregation" should "be possible in stochastic NSGA" in {

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
      objective = Seq(a evaluate f in (0.0 to 1.0 by 0.1), b in (0.2 to 0.5 by 0.1)),
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
      objective = Seq(a evaluate f under 9, b under 3.0),
      genome = Seq(a in (0.0, 1.0)),
      termination = 100,
      stochastic = Stochastic()
    )
  }

  "OMRHook" should "work with NSGA" in:
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

    nsga hook ("/tmp/test")

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

    val wf: DSL = nsga hook ("/tmp/test")
    val wf2: DSL = nsga hook ("/tmp/test") by Island(100)

    // FIXME improve this test when more metadata are added to EvolutioWorkflow
    tasks(wf).flatMap(_.task.name).exists(_.contains("island")) should equal(false)
    tasks(wf2).flatMap(_.task.name).exists(_.contains("island")) should equal(true)
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

  "by Island" should "be usable with by" in:
    val a = Val[Double]
    val b = Val[Double]

    val p: DSL =
      NSGA2Evolution(
        evaluation = EmptyTask() set (inputs += a, outputs += b),
        objective = Seq(b),
        genome = Seq(a in (0.0, 1.0)),
        termination = 100
      ) on LocalEnvironment(1) by Island(10) by 10


  "OSE" should "support enumerations" in:
    val e = Val[WorkflowSpec.En]
    val a = Val[Double]
    val b = Val[Double]

    def f(v: Vector[Double]) = v.head

    OSEEvolution(
      origin = Seq(e in WorkflowSpec.En.values),
      evaluation = EmptyTask(),
      objective = Seq(a evaluate f under 9, b under 3.0),
      genome = Seq(a in(0.0, 1.0)),
      termination = 100,
      stochastic = Stochastic()
    )


  "HDOSE" should "support syntax for method definition" in :
    val ar = Val[Array[Double]]
    val i1 = Val[Int]
    val a = Val[Double]
    val b = Val[Double]
    val ba = Val[Array[Boolean]]
    val bi = Val[Array[Int]]

    val testTask =
      FromContextTask("test") { p =>
        import p._
        context(ba).size should equal(10)
        context(bi).size should equal(10)
        context
      } set ((inputs, outputs) += (ba, bi), outputs += (a, b), a := 8, b := 1.0)


    val hdose =
      HDOSEEvolution(
        origin = Seq(
          ar in Vector.fill(10)(0.0 to 10.0 weight 2.0),
          i1 in (0.0 to 10.0 weight 5.0),
          ba in Seq.fill(10)(TrueFalse),
          bi in Seq.fill(10)(1 to 100)
        ),
        evaluation = testTask,
        objective = Seq(a under 9, b under 3.0),
        termination = 100,
        stochastic = Stochastic()
      )

    hdose.run()

  it should "be serializable" in:
    val a = Val[Double]
    val b = Val[Double]
    val exactObjectives = Objectives.toExact(OSE.FitnessPattern.toObjectives( Seq(a under 9, b under 3.0)))
    val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), Seq())

    serializeDeserialize(phenotypeContent)

  "OSE" should "be serializable" in:

    val x = Val[Int]
    val y = Val[Int]
    val z = Val[Double]

    val o1 = Val[Int]
    val o2 = Val[Double]

    val mySeed = Val[Long]

    val model =
      EmptyTask() set (
        inputs += (x, y, z, mySeed),
        outputs += (o1, o2),
        o1 := 28,
        o2 := 72
      )

    val ose =
      OSEEvolution(
        evaluation = model,
        termination = 10,
        origin = Seq(
          x in Seq(1, 2),
          y in (5 to 56 by 1),
          z in (0.01 to 0.2 by 0.01)
        ),
        objective = Seq(
          o1 delta 28 under 0.1,
          o2 delta 72 under 0.1
        ),
        stochastic = Stochastic(seed = mySeed)
      )


    serializeDeserialize(ose).run()



}