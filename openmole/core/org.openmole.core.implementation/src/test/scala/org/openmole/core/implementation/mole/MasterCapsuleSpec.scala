/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.core.implementation.mole

import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data.IContext
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.sampling._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.model.data.DataModeMask._

@RunWith(classOf[JUnitRunner])
class MasterCapsuleSpec extends FlatSpec with ShouldMatchers {

  implicit val plugins = PluginSet.empty

  "A master capsule" should "execute tasks" in {
    val p = new Prototype[String]("p")

    val t1 = new TestTask {
      val name = "Test write"
      override val outputs = DataSet(p)
      override def process(context: IContext) = context + (p -> "Test")
    }

    val t2 = new TestTask {
      val name = "Test read"
      override val inputs = DataSet(p)
      override def process(context: IContext) = {
        context.value(p).get should equal("Test")
        context
      }
    }

    val t1c = new MasterCapsule(t1)
    val t2c = new MasterCapsule(t2)

    new Transition(t1c, t2c)

    new MoleExecution(new Mole(t1c)).start.waitUntilEnded
  }

  "A master capsule" should "keep value of a variable from on execution to another" in {
    val data = List("A", "A", "B", "C")
    val i = new Prototype[String]("i")
    val n = new Prototype[Int]("n")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val emptyT = EmptyTask("Slave")
    emptyT.addInput(i)
    emptyT.addOutput(i)

    val emptyC = new Capsule(emptyT)

    val select = new TestTask {
      val name = "select"
      override val inputs = DataSet(n, i)
      override val outputs = DataSet(n, i)
      override val parameters = ParameterSet(n -> 0)
      override def process(context: IContext) = {
        val nVal = context.value(n).get
        context + new Variable(n, nVal + 1) + new Variable(i, (nVal + 1).toString)
      }
    }

    val selectCaps = new MasterCapsule(select, n)

    new ExplorationTransition(exc, emptyC)
    new Transition(emptyC, selectCaps)
    new Transition(selectCaps, new Slot(emptyC), "n <= 100")

    new MoleExecution(new Mole(exc)).start.waitUntilEnded
  }

  "A end of exploration transition" should "end the master slave process" in {
    @volatile var endCapsExecuted = 0

    val data = List("A", "A", "B", "C")
    val i = new Prototype[String]("i")
    val isaved = new Prototype[Array[String]]("isaved")
    val n = new Prototype[Int]("n")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val emptyT = EmptyTask("Slave")
    emptyT.addInput(i)
    emptyT.addOutput(i)

    val emptyC = new Capsule(emptyT)

    val testT = new TestTask {
      val name = "Test end"
      override val inputs = DataSet(isaved)
      override def process(context: IContext) = {
        context.contains(isaved) should equal(true)
        context.value(isaved).get.size should equal(10)
        endCapsExecuted += 1
        context
      }
    }

    val testC = new Capsule(testT)

    val select = new TestTask {
      val name = "select"
      override val inputs = DataSet(n, isaved, i)
      override val outputs = DataSet(isaved, n, i)
      override val parameters = ParameterSet(n -> 0, isaved -> Array.empty[String])
      override def process(context: IContext) = {
        val nVal = context.value(n).get
        val isavedVar = (nVal.toString :: context.variable(isaved).get.value.toList)

        context +
          (if (isavedVar.size > 10) new Variable(isaved, isavedVar.tail.toArray)
          else new Variable(isaved, isavedVar.toArray)) + new Variable(n, nVal + 1)
      }
    }

    val selectCaps = new MasterCapsule(select, n, isaved)

    new ExplorationTransition(exc, emptyC)
    new Transition(emptyC, selectCaps)
    new Transition(selectCaps, new Slot(emptyC))
    new EndExplorationTransition(selectCaps, testC, "n >= 100")

    new MoleExecution(new Mole(exc)).start.waitUntilEnded
    endCapsExecuted should equal(1)
  }

}
