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
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.test.TestTask
import org.scalatest._

class MasterCapsuleSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "A master capsule" should "execute tasks" in:
    @volatile var testExecuted = false

    val p = Val[String]("p")

    val t1 = TestTask { _ + (p -> "Test") } set (outputs += p)

    val t2 = TestTask { context =>
      context(p) should equal("Test")
      testExecuted = true
      context
    } set (inputs += p)

    val ex = Master(t1) -- Master(t2)

    ex.run

    testExecuted should equal(true)

  it should "keep value of a variable from on execution to another" in {
    val data = List("A", "A", "B", "C")
    val i = Val[String]("i")
    val n = Val[Int]("n")

    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val select = TestTask { context =>
      val nVal = context(n)
      context + Variable(n, nVal + 1) + Variable(i, (nVal + 1).toString)
    } set (
      (inputs, outputs) += (n, i),
      n := 0
    )

    val ex = ExplicitSampling(i, data) -< emptyT -- (Master(select, n) -- Slot(emptyT) when "n <= 100")

    ex.run
  }

  "A end of exploration transition" should "end the master slave process" in {
    @volatile var selectTaskExecuted = 0
    @volatile var endCapsExecuted = 0

    val i = Val[Int]("i")
    val archive = Val[Array[Int]]("archive")

    val sampling = ExplicitSampling(i, 0 until 10)

    val exploration = ExplorationTask(sampling)

    val model =
      EmptyTask() set (
        inputs += i,
        outputs += i
      )

    val select = TestTask { context =>
      assert(context.contains(archive))
      selectTaskExecuted += 1
      context + Variable(archive, (context(i) :: context(archive).toList) toArray)
    } set (
      (inputs, outputs) += (archive, i),
      archive := Array.empty[Int]
    )

    val finalTask = TestTask { context =>
      assert(context.contains(archive))
      assert(context(archive).size >= 10 && context(archive).size < 21)
      endCapsExecuted += 1
      context
    } set (
      inputs += archive
    )

    val ex =
      exploration -< model -- Master(select, archive) &
        select -- Slot(model) &
        (select >| finalTask when "archive.size >= 10")

    ex.run
    endCapsExecuted should equal(1)
  }

  "A master capsule" should "work with mole tasks" in {
    val i = Val[Int]
    val t1 = EmptyTask() set ((inputs, outputs) += i)
    val mt = MoleTask(t1)
    val ex = ExplicitSampling(i, 0 to 100) -< Master(mt, i)
    ex.run
  }

}
