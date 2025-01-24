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

import org.openmole.core.context.Val
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.test.{ TestHook, TestSource, TestTask }
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.validation.DataflowProblem._
import org.openmole.core.workflow.validation.TopologyProblem._
import org.scalatest._

class ValidationSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "Validation" should "detect a missing input error" in {
    val p = Val[String]("t")

    val t1 = EmptyTask()
    val t2 = EmptyTask() set (inputs += p)

    val mole: Mole = t1 -- t2

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty)
    errors.headOption should matchPattern { case Some(MissingInput(_, `p`, _)) => }
  }

  "Validation" should "not detect a missing input error" in {
    val p = Val[String]("t")

    val t1 = EmptyTask()
    val t2 = EmptyTask() set (inputs += p, p := "test")

    val mole: Mole = t1 -- t2

    Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty).isEmpty should be(true)
  }

  "Validation" should "detect a type error" in {
    val pInt = Val[Int]("t")
    val pString = Val[String]("t")

    val t1 = EmptyTask() set (outputs += pInt)
    val t2 = EmptyTask() set (inputs += pString)

    val mole: Mole = t1 -- t2

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty)
    errors.headOption should matchPattern { case Some(WrongType(_, `pString`, `pInt`)) => }
  }

  "Validation" should "detect a topology error" in {
    val p = Val[String]

    val t1 = EmptyTask()
    val t2 = EmptyTask()

    val mole: Mole = t1 -< t2 -- t1

    val errors = Validation.topologyErrors(mole)
    assert(!errors.isEmpty)
  }

  "Validation" should "detect a duplicated transition" in {
    val t1 = EmptyTask()
    val t2 = EmptyTask()

    val mole: Mole = (t1 -- t2) & (t1 -- t2)

    val errors = Validation.duplicatedTransitions(mole)
    assert(!errors.isEmpty)
  }

  "Validation" should "detect a missing input error due to datachannel filtering" in {
    val p = Val[String]

    val t1 = TestTask { _ + (p, "test") } set (outputs += p)

    val t2 = EmptyTask()
    val t3 = EmptyTask() set (inputs += p)

    val mole: Mole = (t1 -- t2 -- t3) & (t1 oo t3 block p)

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty)

    errors.headOption should matchPattern { case Some(MissingInput(_, `p`, _)) => }
  }

  "Validation" should "detect a missing input in the submole" in {
    val p = Val[String]

    val t1 = EmptyTask()
    val t2 = EmptyTask() set (inputs += p)

    val mt = MoleTask(t1 -- t2)

    val errors = Validation(mt)

    errors.headOption should matchPattern { case Some(MoleTaskDataFlowProblem(_, MissingInput(_, `p`, _))) => }

  }

  "Validation" should "detect an incorect level of the last capsule of a mole task" in {
    val t1 = EmptyTask()
    val t2 = EmptyTask()

    val mt = MoleTask(t1 -< t2)

    val errors = Validation(mt)

    errors.headOption should matchPattern { case Some(MoleTaskLastCapsuleProblem(_, _, _)) => }
  }

  "Validation" should "not detect a missing input" in {
    val p = Val[String]

    val t1 = TestTask { _ + (p, "test") } set (outputs += p)
    val t2 = EmptyTask() set (inputs += p)
    val mt = MoleTask(t2)

    val mole = t1 -- mt

    val errors = Validation(mole)
    assert(errors.isEmpty)
  }

  "Validation" should "not detect a missing input when provided by the implicits" in {
    val p = Val[String]

    val t1 = TestTask { _ + (p, "test") } set (outputs += p)

    val t2 = EmptyTask() set (inputs += p)
    val t3 = EmptyTask() set (inputs += p)

    val mt = MoleTask(t2 -- t3) set (implicits += p)

    val mole = t1 -- mt

    val errors = Validation(mole)
    assert(errors.isEmpty)
  }

  "Validation" should "detect a duplicated name error" in {
    val pInt = Val[Int]("t")
    val pString = Val[String]("t")

    val t1 = EmptyTask() set (outputs += (pInt, pString))

    val errors = Validation.duplicatedName(t1, Sources.empty, Hooks.empty)
    errors.headOption should matchPattern { case Some(DuplicatedName(_, _, _, Output)) => }
  }

  "Validation" should "detect a missing input error for the hook" in {
    val i = Val[Int]

    val t1 = EmptyTask()
    val h = TestHook() set (inputs += i)

    val ex: MoleExecution = t1 hook h

    val errors = Validation.hookErrors(ex.mole, hooks = ex.hooks, implicits = List.empty, sources = List.empty)
    errors.headOption should matchPattern { case Some(MissingHookInput(_, _, _)) => }
  }

  "Validation" should "detect a wrong input type error for the misc" in {
    val iInt = Val[Int]("i")
    val iString = Val[String]("i")

    val t1 = EmptyTask() set (outputs += iString)
    val h = TestHook() set (inputs += iInt)

    val ex: MoleExecution = t1 hook h

    val errors = Validation.hookErrors(ex.mole, hooks = ex.hooks, implicits = List.empty, sources = List.empty)
    errors.headOption should matchPattern { case Some(WrongHookType(_, _, _, _)) => }
  }

  "Validation" should "take into account outputs produced by a source" in {
    val t = Val[Int]

    val t1 = EmptyTask() set (inputs += t)

    val s = TestSource() set (outputs += t)
    val ex: MoleExecution = t1 source s

    val errors = Validation.taskTypeErrors(ex.mole)(ex.mole.capsules, Iterable.empty, sources = ex.sources, Hooks.empty)
    assert(errors.isEmpty)
  }

  "Validation" should "detect a missing input for a source" in {
    val t = Val[Int]

    val t1 = EmptyTask()
    val s = TestSource() set (inputs += t)
    val ex: MoleExecution = t1 source s

    val errors = Validation.sourceErrors(ex.mole, List.empty, sources = ex.sources, Hooks.empty)
    errors.headOption should matchPattern { case Some(MissingSourceInput(_, _, _)) => }
  }

  "Validation" should "detect a data channel error when a data channel is going from a level to a lower level" in {
    val i = Val[String]

    val exc = ExplorationTask(EmptySampling())

    val testT = EmptyTask() set (outputs += i)
    val noOP = EmptyTask()
    val aggT = EmptyTask()

    val mole = (exc -< testT -- noOP >- aggT) & (testT oo aggT)

    val errors = Validation.dataChannelErrors(mole)

    errors.headOption should matchPattern { case Some(DataChannelNegativeLevelProblem(_)) => }
  }

  "Merge between aggregation and simple transition" should "be supported" in {
    val j = Val[Int]

    val t1 = EmptyTask() set (outputs += j)

    val exploration =
      ExplorationTask(EmptySampling()) set (
        (inputs, outputs) += j.toArray,
        j.toArray := Array.empty[Int]
      )

    val agg = EmptyTask() set (inputs += j.toArray.toArray)

    val mole = (exploration -< t1 >- agg) & (exploration -- agg)

    assert(Validation(mole).isEmpty)
  }

  "Validation" should "detect a incoherent input in slot" in {
    val pInt = Val[Int]("t")
    val pString = Val[String]("t")

    val t0 = EmptyTask()

    val t1 = EmptyTask() set (outputs += pInt)
    val t2 = EmptyTask() set (outputs += pString)
    val t3 = EmptyTask() set (inputs += pInt)

    val mole = t0 -- (t1, t2) -- t3

    val errors = Validation.incoherentTypeAggregation(mole, Sources.empty, Hooks.empty)
    errors.headOption should matchPattern { case Some(IncoherentTypeAggregation(_, _)) => }
  }

  "Validation" should "detect a incoherent input between slots" in {
    val pInt = Val[Int]("t")
    val pString = Val[String]("t")

    val t0 = EmptyTask()

    val t1 = EmptyTask() set (outputs += pInt)
    val t2 = EmptyTask() set (outputs += pString)
    val t3 = EmptyTask() set (inputs += pInt)

    val mole = t0 -- (t1 -- Slot(t3), t2 -- Slot(t3))

    val errors = Validation.incoherentTypeBetweenSlots(mole, Sources.empty, Hooks.empty)
    errors.headOption should matchPattern { case Some(IncoherentTypesBetweenSlots(_, _, _)) => }
  }

  "Workflow with exploration and strainer" should "be ok" in {
    val pInt = Val[Int]

    val exploration = ExplorationTask(ExplicitSampling(pInt, (0 to 10)))

    val t1 = EmptyTask() set (inputs += pInt)
    val t2 = EmptyTask() set (inputs += pInt)

    val puzzle = Strain(exploration) -< Strain(t1) -- t2

    puzzle run
  }

}
