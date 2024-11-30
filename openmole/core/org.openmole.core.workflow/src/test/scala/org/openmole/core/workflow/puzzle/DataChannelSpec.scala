/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.workflow.puzzle

import org.openmole.core.context.Val
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.test.TestTask
import org.scalatest._

import scala.collection.mutable.ListBuffer

class DataChannelSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "A datachannel" should "enable variable values to be transmitted from a task to another" in {
    val p = Val[String]

    val t1 = TestTask { _ + (p -> "Test") } set (outputs += p)
    val t2 = EmptyTask()

    val t3 =
      TestTask { context =>
        context(p) should equal("Test")
        context
      } set (inputs += p)

    val ex = (t1 -- t2 -- t3) & (t1 oo t3)

    ex.run()
  }

  "A datachannel" should "not conflict with variable values to be transmitted by a transition" in {
    val p = Val[String]

    val t1 = TestTask { _ + (p -> "Test") } set (outputs += p)
    val t2 = TestTask { _ + (p -> "Correct") } set { (inputs, outputs) += p }

    val t3 =
      TestTask { context =>
        context(p) should equal("Correct")
        context
      } set (inputs += p)

    val ex = (t1 -- t2 -- t3) & (t1 oo t3)

    ex.run()
  }

  "A data channel" should "be able to transmit the value to the multiple execution of an explored task" in {
    val j = Val[String]
    val tw = TestTask { _ + (j -> "J") } set (outputs += j)

    val data = List("A", "B", "C")
    val i = Val[String]

    val res = new ListBuffer[String]

    val t = TestTask { context =>
      res.synchronized {
        context.contains(i) should equal(true)
        context.contains(j) should equal(true)
        res += context(i)
      }
      context
    } set (inputs += (i, j))

    val ex = (tw -- ExplicitSampling(i, data) -< t) & (tw oo t)
    ex.run
    res.toVector.sorted should equal(data.toVector)
  }

}
