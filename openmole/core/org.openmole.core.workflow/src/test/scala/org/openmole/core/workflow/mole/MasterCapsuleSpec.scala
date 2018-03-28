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

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.scalatest._
import org.scalatest.junit._

import scala.collection.mutable.ListBuffer
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.dsl._

class MasterCapsuleSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.tools.StubServices._

  "A master capsule" should "execute tasks" in {
    val p = Val[String]("p")

    val t1 = TestTask { _ + (p → "Test") } set (
      name := "Test write",
      outputs += p
    )

    val t2 = TestTask { context ⇒
      context(p) should equal("Test")
      context
    } set (
      name := "Test read",
      inputs += p
    )

    val t1c = MasterCapsule(t1)
    val t2c = MasterCapsule(t2)

    val ex = t1c -- t2c toExecution

    ex.run
  }

  "A master capsule" should "keep value of a variable from on execution to another" in {
    val data = List("A", "A", "B", "C")
    val i = Val[String]("i")
    val n = Val[Int]("n")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val emptyT = EmptyTask() set (
      inputs += i,
      outputs += i
    )

    val select = TestTask { context ⇒
      val nVal = context(n)
      context + Variable(n, nVal + 1) + Variable(i, (nVal + 1).toString)
    } set (
      name := "Select",
      inputs += (n, i),
      outputs += (n, i),
      n := 0
    )

    val emptyC = Capsule(emptyT)
    val slot1 = Slot(emptyC)
    val slot2 = Slot(emptyC)

    val selectCaps = MasterCapsule(select, n)

    val ex = exc -< slot1 -- selectCaps -- (slot2 when "n <= 100")

    ex.run
  }

  "A end of exploration transition" should "end the master slave process" in {
    @volatile var selectTaskExecuted = 0
    @volatile var endCapsExecuted = 0

    val i = Val[Int]("i")
    val archive = Val[Array[Int]]("archive")

    val sampling = new ExplicitSampling(i, 0 until 10)

    val exploration = ExplorationTask(sampling)

    val model =
      EmptyTask() set (
        inputs += i,
        outputs += i
      )

    val modelCapsule = Capsule(model)
    val modelSlot1 = Slot(modelCapsule)
    val modelSlot2 = Slot(modelCapsule)

    val select = TestTask { context ⇒
      assert(context.contains(archive))
      selectTaskExecuted += 1
      context + Variable(archive, (context(i) :: context(archive).toList) toArray)
    } set (
      name := "select",
      inputs += (archive, i),
      outputs += (archive, i),
      archive := Array.empty[Int]
    )

    val selectCaps = MasterCapsule(select, archive)

    val finalTask = TestTask { context ⇒
      assert(context.contains(archive))
      assert(context(archive).size >= 10 && context(archive).size < 21)
      endCapsExecuted += 1
      context
    } set (
      name := "final",
      inputs += archive
    )

    val skel = exploration -< modelSlot1 -- selectCaps
    val loop = selectCaps -- modelSlot2
    val terminate = selectCaps >| (finalTask when "archive.size >= 10")

    val ex = skel & loop & terminate

    (noException shouldBe thrownBy(ex.run))
    endCapsExecuted should equal(1)
  }

  "A master capsule" should "work with mole tasks" in {

    val t1 = EmptyTask() set (name := "Test write")

    val mole = Mole(t1)

    val mt = MoleTask(mole, mole.root)

    val t1c = MasterCapsule(mt)

    val i = Val[Int]("i")

    val explo = ExplorationTask(ExplicitSampling(i, 0 to 100))

    val ex = explo -< t1c

    ex.run
  }

}
