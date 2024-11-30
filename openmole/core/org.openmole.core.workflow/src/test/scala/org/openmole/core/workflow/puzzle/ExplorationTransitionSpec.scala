/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
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

class ExplorationTransitionSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "Exploration transition" should "submit one MoleJob for each value in the sampling" in {
    val data = List("A", "B", "C")
    val i = Val[String]("i")

    val sampling = ExplicitSampling(i, data)
    val res = ListBuffer[String]()

    val t = TestTask { context =>
      res.synchronized {
        context.contains(i) should equal(true)
        res += context(i)
        context
      }
    } set (inputs += i)

    val ex = sampling -< t
    ex.run()
    res.toVector.sorted should equal(data.toVector)
  }

  "Exploration transition" should "work with the DSL interface" in {
    val data = List("A", "B", "C")
    val i = Val[String]("i")

    val res = new ListBuffer[String]

    val t = TestTask { context =>
      res.synchronized {
        context.contains(i) should equal(true)
        res += context(i)
        context
      }
    } set (inputs += i)

    (ExplicitSampling(i, data) -< t).run()
    res.toVector.sorted should equal(data.toVector)
  }

  "When keyword in exploration transition" should "should filter some values in the sampling" in {
    val i = Val[Int]("i")
    val data = (1 to 100)

    val sampling = ExplicitSampling(i, data)

    val res = new ListBuffer[Int]

    val t = TestTask { context =>
      res.synchronized {
        context.contains(i) should equal(true)
        res += context(i)
        context
      }
    } set (inputs += i)

    val ex = (sampling -< t when "i % 2 != 0")
    ex.run()
    res.toVector.sorted should equal(data.toVector.filter(_ % 2 != 0))
  }
}
