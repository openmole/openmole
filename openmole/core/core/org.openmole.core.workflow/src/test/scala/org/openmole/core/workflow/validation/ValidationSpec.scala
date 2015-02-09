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

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.validation._
import DataflowProblem._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._

import org.scalatest._
import TopologyProblem.DataChannelNegativeLevelProblem

class ValidationSpec extends FlatSpec with Matchers {

  implicit val plugins = PluginSet.empty

  "Validation" should "detect a missing input error" in {
    val p = Prototype[String]("t")

    val t1 = EmptyTask()
    val t2 = EmptyTask()
    t2 addInput p

    val c1 = Capsule(t1)
    val c2 = Capsule(t2)

    val mole = c1 -- c2

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty)
    errors.headOption match {
      case Some(MissingInput(_, d)) ⇒ assert(d.prototype == p)
      case _                        ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "not detect a missing input error" in {
    val p = Prototype[String]("t")

    val t1 = EmptyTask()
    val t2 = EmptyTask()
    t2 addInput p
    t2 setDefault (p, "Test")

    val c1 = Capsule(t1)
    val c2 = Capsule(t2)

    val mole = c1 -- c2

    Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty).isEmpty should equal(true)
  }

  "Validation" should "detect a type error" in {
    val pInt = Prototype[Int]("t")
    val pString = Prototype[String]("t")

    val t1 = EmptyTask()
    t1 addOutput pInt

    val t2 = EmptyTask()
    t2 addInput pString

    val c1 = Capsule(t1)
    val c2 = Capsule(t2)

    val mole = c1 -- c2

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty)
    errors.headOption match {
      case Some(WrongType(_, d, t)) ⇒
        assert(d.prototype == pString)
        assert(t == pInt)
      case _ ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a topology error" in {
    val p = Prototype[String]("t")

    val t1 = EmptyTask()
    val t2 = EmptyTask()

    val c1 = Slot(t1)
    val c2 = Capsule(t2)

    val mole = c1 -< c2 -- c1

    val errors = Validation.topologyErrors(mole)
    errors.isEmpty should equal(false)
  }

  "Validation" should "detect a duplicated transition" in {
    val t1 = EmptyTask()
    val t2 = EmptyTask()

    val c1 = Capsule(t1)
    val c2 = Slot(t2)

    val mole = (c1 -- c2) + (c1 -- c2)

    val errors = Validation.duplicatedTransitions(mole)
    errors.isEmpty should equal(false)
  }

  "Validation" should "detect a missing input error due to datachannel filtering" in {
    val p = Prototype[String]("t")

    val t1 =
      new TestTask {
        val name = "t1"
        override def outputs = DataSet(p)
        override def process(context: Context) = Context(Variable(p, "test"))
      }

    val t2 = EmptyTask()
    val t3 = EmptyTask()
    t3 addInput p

    val c1 = Capsule(t1)
    val c2 = Capsule(t2)
    val c3 = Slot(t3)

    val mole = (c1 -- c2 -- c3) + (c1 oo (c3, Block(p)))

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources.empty, Hooks.empty)

    errors.headOption match {
      case Some(MissingInput(_, d)) ⇒ assert(d.prototype == p)
      case _                        ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a missing input in the submole" in {
    val p = Prototype[String]("t")

    val t1 = EmptyTask()

    val t2 = EmptyTask()
    t2 addInput p

    val c1 = Capsule(t1)
    val c2 = Capsule(t2)

    val mt = MoleTask(c1 -- c2)

    val errors = Validation(Mole(mt))

    errors.headOption match {
      case Some(MoleTaskDataFlowProblem(_, MissingInput(_, d))) ⇒ assert(d.prototype == p)
      case _ ⇒ sys.error("Error should have been detected")
    }

  }

  "Validation" should "not detect a missing input" in {
    val p = Prototype[String]("t")

    val t1 =
      new TestTask {
        val name = "t1"
        override def outputs = DataSet(p)
        override def process(context: Context) = Context(Variable(p, "test"))
      }

    val c1 = Capsule(t1)

    val t2 = EmptyTask()
    t2 addInput p
    val c2 = Capsule(t2)

    val mt = MoleTask(c2)

    val mtC = Capsule(mt)

    val mole = c1 -- mtC

    val errors = Validation(mole)
    errors.isEmpty should equal(true)
  }

  "Validation" should "not detect a missing input when provided by the implicits" in {
    val p = Prototype[String]("t")

    val t1 =
      new TestTask {
        val name = "t1"
        override def outputs = DataSet(p)
        override def process(context: Context) = Context(Variable(p, "test"))
      }

    val c1 = Capsule(t1)

    val t2 = EmptyTask()
    t2 addInput p
    val c2 = Capsule(t2)

    val t3 = EmptyTask()
    t3 addInput p
    val c3 = Capsule(t3)

    val mt = MoleTask(c2 -- c3)
    mt addImplicit p

    val mtC = Capsule(mt)

    val mole = c1 -- mtC

    val errors = Validation(mole)
    errors.isEmpty should equal(true)
  }

  "Validation" should "detect a duplicated name error" in {
    val pInt = Prototype[Int]("t")
    val pString = Prototype[String]("t")

    val t1 = EmptyTask()
    t1 addOutput pInt
    t1 addOutput pString

    val c1 = Capsule(t1)

    val errors = Validation.duplicatedName(Mole(c1), Sources.empty, Hooks.empty)
    errors.headOption match {
      case Some(DuplicatedName(_, _, _, Output)) ⇒
      case _                                     ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a missing input error for the misc" in {
    val i = Prototype[Int]("t")

    val t1 = EmptyTask()

    val c1 = Capsule(t1)

    val h = new HookBuilder {
      addInput(i)

      def toHook = new Hook with Built {
        def process(ctx: Context, executionContext: ExecutionContext) = ctx
      }
    }

    val errors = Validation.hookErrors(Mole(c1), Iterable.empty, Sources.empty, Hooks(Map(c1 -> List(h))))
    errors.headOption match {
      case Some(MissingHookInput(_, _, _)) ⇒
      case _                               ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a wrong input type error for the misc" in {
    val iInt = Prototype[Int]("i")
    val iString = Prototype[String]("i")

    val t1 = EmptyTask()
    t1 addOutput iString

    val c1 = Capsule(t1)

    val h = new HookBuilder {
      addInput(iInt)

      def toHook = new Hook with Built {
        def process(ctx: Context, executionContext: ExecutionContext) = ctx
      }
    }

    val errors = Validation.hookErrors(Mole(c1), Iterable.empty, Sources.empty, Hooks(Map(c1 -> List(h))))
    errors.headOption match {
      case Some(WrongHookType(_, _, _, _)) ⇒
      case _                               ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "take into account outputs produced by a source" in {
    val t = Prototype[Int]("t")

    val t1 = EmptyTask()
    t1 addInput t

    val c1 = Capsule(t1)

    val s = new SourceBuilder {
      addOutput(t)

      def toSource = new Source with Built {
        def process(ctx: Context, executionContext: ExecutionContext) = Context.empty
      }
    }.toSource

    val mole = Mole(c1)

    val errors = Validation.taskTypeErrors(mole)(mole.capsules, Iterable.empty, Sources(Map(c1 -> List(s))), Hooks.empty)
    errors.isEmpty should equal(true)
  }

  "Validation" should "detect a missing input for a source" in {
    val t = Prototype[Int]("t")

    val t1 = EmptyTask()

    val c1 = Capsule(t1)

    val s = new SourceBuilder {
      addInput(t)

      def toSource = new Source with Built {
        def process(ctx: Context, executionContext: ExecutionContext) = Context.empty
      }
    }.toSource

    val mole = Mole(c1)

    val errors = Validation.sourceTypeErrors(mole, List.empty, Sources(Map(c1 -> List(s))), Hooks.empty)
    errors.headOption match {
      case Some(MissingSourceInput(_, _, _)) ⇒
      case _                                 ⇒ sys.error("Error should have been detected")
    }
  }

  "Validation" should "detect a data channel error when a data channel is going from a level to a lower level" in {
    val i = Prototype[String]("i")

    val exc = Capsule(ExplorationTask(new EmptySampling))

    val testT = EmptyTask()
    testT addOutput i

    val noOP = EmptyTask()
    val aggT = EmptyTask()

    val testC = Capsule(testT)
    val noOPC = Capsule(noOP)
    val aggC = Slot(aggT)

    val mole = (exc -< testC -- noOPC >- aggC) + (testC oo aggC)

    val errors = Validation.dataChannelErrors(mole)
    errors.headOption match {
      case Some(DataChannelNegativeLevelProblem(_)) ⇒
      case _                                        ⇒ sys.error("Error should have been detected")
    }
  }

  "Merge between aggregation and simple transition" should "be supported" in {
    val j = Prototype[Int]("j")

    val t1 = EmptyTask()
    t1 addOutput j

    val t1Caps = Capsule(t1)

    val exploration = ExplorationTask(new EmptySampling)
    exploration addInput j.toArray
    exploration addOutput j.toArray
    exploration setDefault (j.toArray, Array.empty[Int])

    val explorationCaps = Capsule(exploration)

    val agg = EmptyTask()
    agg addInput j.toArray.toArray

    val aggSlot = Slot(agg)

    val mole = (explorationCaps -< t1Caps >- aggSlot) + (explorationCaps -- aggSlot)
    Validation(mole).isEmpty should equal(true)
  }

}
