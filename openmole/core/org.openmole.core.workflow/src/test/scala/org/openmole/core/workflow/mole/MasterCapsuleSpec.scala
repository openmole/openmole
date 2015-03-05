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

package org.openmole.core.workflow.mole

import org.openmole.core.workflow.builder.InputOutputBuilder
import org.openmole.core.workflow.execution.local.LocalEnvironment
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.scalatest._
import org.scalatest.junit._
import scala.collection.mutable.ListBuffer
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._

class MasterCapsuleSpec extends FlatSpec with Matchers {

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

    val t1c = MasterCapsule(t1)
    val t2c = MasterCapsule(t2)

    val ex = t1c -- t2c toExecution

    ex.start.waitUntilEnded
  }

  "A master capsule" should "keep value of a variable from on execution to another" in {
    val data = List("A", "A", "B", "C")
    val i = Prototype[String]("i")
    val n = Prototype[Int]("n")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val emptyT = EmptyTask()
    emptyT.addInput(i)
    emptyT.addOutput(i)

    val select = new TestTask {
      val name = "select"
      override val inputs = DataSet(n, i)
      override val outputs = DataSet(n, i)
      override val defaults = DefaultSet(Default(n, 0))
      override def process(context: Context) = {
        val nVal = context(n)
        context + Variable(n, nVal + 1) + Variable(i, (nVal + 1).toString)
      }
    }

    val emptyC = Capsule(emptyT)
    val slot1 = Slot(emptyC)
    val slot2 = Slot(emptyC)

    val selectCaps = MasterCapsule(select, n)

    val ex = exc -< slot1 -- selectCaps -- (slot2, "n <= 100")

    ex.start.waitUntilEnded
  }

  "A end of exploration transition" should "end the master slave process" in {
    val local = LocalEnvironment(1)

    @volatile var selectTaskExecuted = 0
    @volatile var endCapsExecuted = 0

    val i = Prototype[Int]("i")
    val archive = Prototype[Array[Int]]("archive")

    val sampling = new ExplicitSampling(i, 0 until 10)

    val exploration = ExplorationTask(sampling)

    val model = EmptyTask()
    model addInput i
    model addOutput i

    val modelCapsule = Capsule(model)
    val modelSlot1 = Slot(modelCapsule)
    val modelSlot2 = Slot(modelCapsule)

    val select = new TestTask {
      val name = "select"
      override val inputs = DataSet(archive, i)
      override val outputs = DataSet(archive, i)
      override val defaults = DefaultSet(Default(archive, Array.empty[Int]))
      override def process(context: Context) = {
        context.contains(archive) should equal(true)
        selectTaskExecuted += 1
        context + Variable(archive, (context(i) :: context(archive).toList) toArray)
      }
    }

    val selectCaps = MasterCapsule(select, archive)

    val finalTask = new TestTask {
      val name = "final"
      override val inputs = DataSet(archive)
      override def process(context: Context) = {
        context.contains(archive) should equal(true)
        context(archive).size should equal(1)
        endCapsExecuted += 1
        context
      }
    }

    val skel = exploration -< modelSlot1 -- selectCaps
    val loop = selectCaps -- modelSlot2
    val terminate = selectCaps >| (finalTask, "archive.size() >= 1")

    val ex = skel + loop + terminate

    ex.toExecution(defaultEnvironment = LocalEnvironment(1)).start.waitUntilEnded
    (selectTaskExecuted < 10) should equal(true)
    endCapsExecuted should equal(1)
  }

  "A master capsule" should "work with mole tasks" in {

    val t1 = new TestTask {
      val name = "Test write"
      override def process(context: Context) = context
    }

    val mole = Mole(t1)

    val mt = MoleTask(mole, mole.root)

    val t1c = MasterCapsule(mt.toTask)

    val i = Prototype[Int]("i")

    val explo = ExplorationTask(new ExplicitSampling(i, 0 to 100))

    val ex = explo -< t1c

    ex.start.waitUntilEnded
  }

}
