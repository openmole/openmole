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

package org.openmole.core.workflow.mole

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.EmptyTask
import org.openmole.core.workflow.transition._
import org.scalatest._

class StrainerCapsuleSpec extends FlatSpec with Matchers {

  "The strainer capsule" should "let the data pass through" in {
    val p = Prototype[String]("p")

    val t1 = new TestTask {
      val name = "Test write"
      override def outputs = DataSet(p)
      override def process(context: Context) = context + (p -> "Test")
    }

    val strainer = EmptyTask()

    val t2 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(p)
      override def process(context: Context) = {
        context(p) should equal("Test")
        context
      }
    }

    val t1c = Capsule(t1)
    val strainerC = StrainerCapsule(strainer)
    val t2c = Capsule(t2)

    val ex = t1c -- strainerC -- t2c
    ex.start.waitUntilEnded
  }

  "The strainer capsule" should "let the data pass through even if linked with a data channel to the root" in {
    val p = Prototype[String]("p")

    val root = StrainerCapsule(EmptyTask())

    val t1 = new TestTask {
      val name = "Test write"
      override def outputs = DataSet(p)
      override def process(context: Context) = context + (p -> "Test")
    }

    val tNone = StrainerCapsule(EmptyTask())
    val tNone2 = StrainerCapsule(EmptyTask())

    val strainer = EmptyTask()

    val t2 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(p)
      override def process(context: Context) = {
        context(p) should equal("Test")
        context
      }
    }

    val strainerC = Slot(StrainerCapsule(strainer))

    val ex = (root -- tNone -- (t1, tNone2) -- strainerC -- t2) + (root oo strainerC)
    ex.start.waitUntilEnded
  }

}
