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
import org.openmole.core.workflow.puzzle._

class MasterCapsuleSpec extends FlatSpec with Matchers {

  "A master capsule" should "execute tasks" in {
    val p = Prototype[String]("p")

    val t1 = TestTask { _ + (p -> "Test") }
    t1 setName "Test write"
    t1 addOutput p

    val t2 = TestTask { context ⇒
      context(p) should equal("Test")
      context
    }
    t2 setName "Test read"
    t2 addInput p

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

    val select = TestTask { context ⇒
      val nVal = context(n)
      context + Variable(n, nVal + 1) + Variable(i, (nVal + 1).toString)
    }
    select setName "select"
    select addInput (n, i)
    select addOutput (n, i)
    select setDefault Default.value(n, 0)

    val emptyC = Capsule(emptyT)
    val slot1 = Slot(emptyC)
    val slot2 = Slot(emptyC)

    val selectCaps = MasterCapsule(select, n)

    val ex = exc -< slot1 -- selectCaps -- (slot2, "n <= 100")

    ex.start.waitUntilEnded
  }

  "A end of exploration transition" should "end the master slave process" in {
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

    val select = TestTask { context ⇒
      context.contains(archive) should equal(true)
      selectTaskExecuted += 1
      context + Variable(archive, (context(i) :: context(archive).toList) toArray)
    }
    select setName "select"
    select addInput (archive, i)
    select addOutput (archive, i)
    select setDefault Default.value(archive, Array.empty[Int])

    val selectCaps = MasterCapsule(select, archive)

    val finalTask = TestTask { context ⇒
      context.contains(archive) should equal(true)
      context(archive).size should equal(1)
      endCapsExecuted += 1
      context
    }
    finalTask setName "final"
    finalTask addInput (archive)

    val skel = exploration -< modelSlot1 -- selectCaps
    val loop = selectCaps -- modelSlot2
    val terminate = selectCaps >| (finalTask, "archive.size >= 1")

    val ex = skel & loop & terminate

    ex.toExecution(defaultEnvironment = LocalEnvironment(1)).start.waitUntilEnded
    //(selectTaskExecuted < 10) should equal(true)
    endCapsExecuted should equal(1)
  }

  "A master capsule" should "work with mole tasks" in {

    val t1 = EmptyTask()
    t1 setName "Test write"

    val mole = Mole(t1)

    val mt = MoleTask(mole, mole.root)

    val t1c = MasterCapsule(mt.toTask)

    val i = Prototype[Int]("i")

    val explo = ExplorationTask(new ExplicitSampling(i, 0 to 100))

    val ex = explo -< t1c

    ex.start.waitUntilEnded
  }

}
