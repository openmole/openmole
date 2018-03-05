/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.workflow.validation

import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.validation._
import DataflowProblem._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.scalatest._
import TopologyProblem.DataChannelNegativeLevelProblem
import org.openmole.core.context.Val

class ValidationSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.tools.StubServices._

  "Validation" should "detect a missing input error" in {
    val p = Val[String]("t")

    val t1 = EmptyTask()
    val t2 = EmptyTask() set (inputs += p)

    val c1 = Capsule(t1)
    val c2 = Capsule(t2)

    val mole = (c1 -- c2).toMole

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty)
    errors.headOption match {
      case Some(MissingInput(_, d)) ⇒ assert(d == p)
      case _                        ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "not detect a missing input error" in {
    val p = Val[String]("t")

    val t1 = EmptyTask()
    val t2 = EmptyTask() set (
      inputs += p,
      p := "Test"
    )

    val c1 = Capsule(t1)
    val c2 = Capsule(t2)

    val mole = (c1 -- c2) toMole

    Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty).isEmpty should equal(true)
  }

  "Validation" should "detect a type error" in {
    val pInt = Val[Int]("t")
    val pString = Val[String]("t")

    val t1 = EmptyTask() set (outputs += pInt)
    val t2 = EmptyTask() set (inputs += pString)

    val c1 = Capsule(t1)
    val c2 = Capsule(t2)

    val mole = (c1 -- c2) toMole

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty)
    errors.headOption match {
      case Some(WrongType(_, d, t)) ⇒
        assert(d == pString)
        assert(t == pInt)
      case _ ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a topology error" in {
    val p = Val[String]("t")

    val t1 = EmptyTask()
    val t2 = EmptyTask()

    val c1 = Slot(t1)
    val c2 = Capsule(t2)

    val mole = (c1 -< c2 -- c1) toMole

    val errors = Validation.topologyErrors(mole)
    errors.isEmpty should equal(false)
  }

  "Validation" should "detect a duplicated transition" in {
    val t1 = EmptyTask()
    val t2 = EmptyTask()

    val c1 = Capsule(t1)
    val c2 = Slot(t2)

    val mole = ((c1 -- c2) & (c1 -- c2)) toMole

    val errors = Validation.duplicatedTransitions(mole)
    errors.isEmpty should equal(false)
  }

  "Validation" should "detect a missing input error due to datachannel filtering" in {
    val p = Val[String]("t")

    val t1 = TestTask { _ + (p, "test") } set (
      name := "t1",
      outputs += p
    )

    val t2 = EmptyTask()
    val t3 = EmptyTask() set (inputs += p)

    val c1 = Capsule(t1)
    val c2 = Capsule(t2)
    val c3 = Slot(t3)

    val mole = ((c1 -- c2 -- c3) & (c1 oo (c3, Block(p)))) toMole

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty)

    errors.headOption match {
      case Some(MissingInput(_, d)) ⇒ assert(d == p)
      case _                        ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a missing input in the submole" in {
    val p = Val[String]("t")

    val t1 = EmptyTask()

    val t2 = EmptyTask() set (inputs += p)

    val c1 = Capsule(t1)
    val c2 = Capsule(t2)

    val mt = MoleTask(c1 -- c2)

    val errors = Validation(Mole(mt))

    errors.headOption match {
      case Some(MoleTaskDataFlowProblem(_, MissingInput(_, d))) ⇒ assert(d == p)
      case _ ⇒ sys.error("Error should have been detected")
    }

  }

  "Validation" should "not detect a missing input" in {
    val p = Val[String]("t")

    val t1 = TestTask { _ + (p, "test") } set (
      name := "t1",
      outputs += p
    )

    val c1 = Capsule(t1)

    val t2 = EmptyTask() set (inputs += p)
    val c2 = Capsule(t2)

    val mt = MoleTask(c2)

    val mtC = Capsule(mt)

    val mole = (c1 -- mtC) toMole

    val errors = Validation(mole)
    errors.isEmpty should equal(true)
  }

  "Validation" should "not detect a missing input when provided by the implicits" in {
    val p = Val[String]("t")

    val t1 = TestTask { _ + (p, "test") } set (
      name := "t1",
      outputs += p
    )

    val c1 = Capsule(t1)

    val t2 = EmptyTask() set (inputs += p)
    val c2 = Capsule(t2)

    val t3 = EmptyTask() set (inputs += p)
    val c3 = Capsule(t3)

    val mt = MoleTask(c2 -- c3) set (implicits += p)

    val mtC = Capsule(mt)

    val mole = (c1 -- mtC) toMole

    val errors = Validation(mole)
    errors.isEmpty should equal(true)
  }

  "Validation" should "detect a duplicated name error" in {
    val pInt = Val[Int]("t")
    val pString = Val[String]("t")

    val t1 = EmptyTask() set (outputs += (pInt, pString))

    val c1 = Capsule(t1)

    val errors = Validation.duplicatedName(Mole(c1), Sources.empty, Hooks.empty)
    errors.headOption match {
      case Some(DuplicatedName(_, _, _, Output)) ⇒
      case _                                     ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a missing input error for the misc" in {
    val i = Val[Int]("t")

    val t1 = EmptyTask()

    val c1 = Capsule(t1)

    val h = TestHook() set (inputs += i)

    val errors = Validation.hookErrors(Mole(c1), Iterable.empty, Sources.empty, Hooks(Map(c1 → List(h))))
    errors.headOption match {
      case Some(MissingHookInput(_, _, _)) ⇒
      case _                               ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a wrong input type error for the misc" in {
    val iInt = Val[Int]("i")
    val iString = Val[String]("i")

    val t1 = EmptyTask() set (outputs += iString)

    val c1 = Capsule(t1)

    val h = TestHook() set (inputs += iInt)

    val errors = Validation.hookErrors(Mole(c1), Iterable.empty, Sources.empty, Hooks(Map(c1 → List(h))))
    errors.headOption match {
      case Some(WrongHookType(_, _, _, _)) ⇒
      case _                               ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "take into account outputs produced by a source" in {
    val t = Val[Int]("t")

    val t1 = EmptyTask() set (inputs += t)

    val c1 = Capsule(t1)

    val s = TestSource() set (outputs += t)

    val mole = Mole(c1)

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources(Map(c1 → List(s))), Hooks.empty)
    errors.isEmpty should equal(true)
  }

  "Validation" should "detect a missing input for a source" in {
    val t = Val[Int]("t")

    val t1 = EmptyTask()

    val c1 = Capsule(t1)

    val s = TestSource() set (inputs += t)

    val mole = Mole(c1)

    val errors = Validation.sourceTypeErrors(mole, List.empty, Sources(Map(c1 → List(s))), Hooks.empty)
    errors.headOption match {
      case Some(MissingSourceInput(_, _, _)) ⇒
      case _                                 ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a data channel error when a data channel is going from a level to a lower level" in {
    val i = Val[String]("i")

    val exc = Capsule(ExplorationTask(new EmptySampling))

    val testT = EmptyTask() set (outputs += i)

    val noOP = EmptyTask()
    val aggT = EmptyTask()

    val testC = Capsule(testT)
    val noOPC = Capsule(noOP)
    val aggC = Slot(aggT)

    val mole = ((exc -< testC -- noOPC >- aggC) & (testC oo aggC)) toMole

    val errors = Validation.dataChannelErrors(mole)

    errors.headOption match {
      case Some(DataChannelNegativeLevelProblem(_)) ⇒
      case _                                        ⇒ sys.error("Error should have been detected")
    }
  }

  "Merge between aggregation and simple transition" should "be supported" in {
    val j = Val[Int]("j")

    val t1 = EmptyTask() set (outputs += j)

    val t1Caps = Capsule(t1)

    val exploration = ExplorationTask(new EmptySampling) set (
      (inputs, outputs) += j.toArray,
      j.toArray := Array.empty[Int]
    )

    val explorationCaps = Capsule(exploration)

    val agg = EmptyTask() set (inputs += j.toArray.toArray)

    val aggSlot = Slot(agg)

    val mole = ((explorationCaps -< t1Caps >- aggSlot) & (explorationCaps -- aggSlot)) toMole

    Validation(mole).isEmpty should equal(true)
  }

  "Validation" should "detect a incoherent input in slot" in {
    val pInt = Val[Int]("t")
    val pString = Val[String]("t")

    val t0 = EmptyTask()

    val t1 = EmptyTask() set (outputs += pInt)
    val t2 = EmptyTask() set (outputs += pString)
    val t3 = EmptyTask() set (inputs += pInt)

    val mole = (t0 -- (t1, t2) -- t3).toMole

    val errors = Validation.incoherentTypeAggregation(mole, Sources.empty, Hooks.empty)
    errors.headOption match {
      case Some(IncoherentTypeAggregation(_, _)) ⇒
      case _                                     ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a incoherent input between slots" in {
    val pInt = Val[Int]("t")
    val pString = Val[String]("t")

    val t0 = EmptyTask()

    val t1 = EmptyTask() set (outputs += pInt)
    val t2 = EmptyTask() set (outputs += pString)
    val t3 = EmptyTask() set (inputs += pInt)

    val mole = (t0 -- (t1, t2) --= t3).toMole

    val errors = Validation.incoherentTypeBetweenSlots(mole, Sources.empty, Hooks.empty)
    errors.headOption match {
      case Some(IncoherentTypesBetweenSlots(_, _, _)) ⇒
      case _ ⇒ sys.error("Error should have been detected")
    }
  }

  "Workflow with exploration and strainer" should "be ok" in {
    val pInt = Val[Int]("t")

    val exploration = ExplorationTask(ExplicitSampling(pInt, (0 to 10)))

    val t1 = EmptyTask() set (inputs += pInt)
    val t2 = EmptyTask() set (inputs += pInt)

    val puzzle = (Capsule(exploration, strain = true) -< Capsule(t1, strain = true) -- t2)

    puzzle run
  }

}
