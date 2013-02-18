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

package org.openmole.core.implementation.mole

import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.sampling._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._

@RunWith(classOf[JUnitRunner])
class MasterCapsuleSpec extends FlatSpec with ShouldMatchers {

  implicit val plugins = PluginSet.empty

  "A master capsule" should "execute tasks" in {
    val p = Prototype[String]("p")

    val t1 = new TestTask {
      val name = "Test write"
      override val outputs = DataSet(p)
      override def process(context: Context) = context + (p -> "Test")
    }

    val t2 = new TestTask {
      val name = "Test read"
      override val inputs = DataSet(p)
      override def process(context: Context) = {
        context(p) should equal("Test")
        context
      }
    }

    val t1c = new MasterCapsule(t1)
    val t2c = new MasterCapsule(t2)

    val ex = t1c -- t2c toExecution

    ex.start.waitUntilEnded
  }

  "A master capsule" should "keep value of a variable from on execution to another" in {
    val data = List("A", "A", "B", "C")
    val i = Prototype[String]("i")
    val n = Prototype[Int]("n")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val emptyT = EmptyTask("Slave")
    emptyT.addInput(i)
    emptyT.addOutput(i)

    val select = new TestTask {
      val name = "select"
      override val inputs = DataSet(n, i)
      override val outputs = DataSet(n, i)
      override val parameters = ParameterSet(n -> 0)
      override def process(context: Context) = {
        val nVal = context(n)
        context + Variable(n, nVal + 1) + Variable(i, (nVal + 1).toString)
      }
    }

    val emptyC = new Capsule(emptyT)
    val slot1 = Slot(emptyC)
    val slot2 = Slot(emptyC)

    val selectCaps = MasterCapsule(select, n)

    val ex = exc -< slot1 -- selectCaps -- (slot2, "n <= 100")

    ex.start.waitUntilEnded
  }

  "A end of exploration transition" should "end the master slave process" in {
    @volatile var endCapsExecuted = 0

    val data = List("A", "A", "B", "C")
    val i = Prototype[String]("i")
    val isaved = Prototype[Array[String]]("isaved")
    val n = Prototype[Int]("n")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val emptyT = EmptyTask("Slave")
    emptyT.addInput(i)
    emptyT.addOutput(i)

    val testT = new TestTask {
      val name = "Test end"
      override val inputs = DataSet(isaved)
      override def process(context: Context) = {
        context.contains(isaved) should equal(true)
        context(isaved).size should equal(10)
        endCapsExecuted += 1
        context
      }
    }

    val select = new TestTask {
      val name = "select"
      override val inputs = DataSet(n, isaved, i)
      override val outputs = DataSet(isaved, n, i)
      override val parameters = ParameterSet(n -> 0, isaved -> Array.empty[String])
      override def process(context: Context) = {
        val nVal = context(n)
        val isavedVar = (nVal.toString :: context.variable(isaved).get.value.toList)

        context +
          (if (isavedVar.size > 10) Variable(isaved, isavedVar.tail.toArray)
          else Variable(isaved, isavedVar.toArray)) + Variable(n, nVal + 1)
      }
    }

    val selectCaps = MasterCapsule(select, n, isaved)

    val emptyC = Capsule(emptyT)
    val slot1 = Slot(emptyC)
    val slot2 = Slot(emptyC)

    val testC = Capsule(testT)

    val ex = (exc -< slot1 -- selectCaps -- slot2) + (selectCaps >| (testC, "n >= 100"))

    ex.start.waitUntilEnded
    endCapsExecuted should equal(1)
  }

  "A master capsule" should "work with mole tasks" in {

    val t1 = new TestTask {
      val name = "Test write"
      override def process(context: Context) = context
    }

    val mole = t1 toMole

    val mt = MoleTask("MoleTask", mole, mole.root, Iterable.empty)

    val t1c = new MasterCapsule(mt.toTask)

    val i = Prototype[Int]("i")

    val explo = ExplorationTask("Exploration", new ExplicitSampling(i, 0 to 100))

    val ex = explo -< t1c

    ex.start.waitUntilEnded
  }

}
