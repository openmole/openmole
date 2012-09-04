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

package org.openmole.core.implementation.mole

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.model.data._
import org.openmole.core.model.transition._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class StrainerCapsuleSpec extends FlatSpec with ShouldMatchers {

  "The strainer capsule" should "let the data pass through" in {
    val p = Prototype[String]("p")

    val t1 = new TestTask {
      val name = "Test write"
      override def outputs = DataSet(p)
      override def process(context: Context) = context + (p -> "Test")
    }

    val strainer = EmptyTask("Strainer")

    val t2 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(p)
      override def process(context: Context) = {
        context.value(p).get should equal("Test")
        context
      }
    }

    val t1c = new Capsule(t1)
    val strainerC = new StrainerCapsule(strainer)
    val t2c = new Capsule(t2)

    val ex = t1c -- strainerC -- t2c
    ex.start.waitUntilEnded
  }

  "The strainer capsule" should "let the data pass through even if linked with a data channel to the root" in {
    val p = Prototype[String]("p")

    val root = new StrainerCapsule(EmptyTask("root"))

    val t1 = new TestTask {
      val name = "Test write"
      override def outputs = DataSet(p)
      override def process(context: Context) = context + (p -> "Test")
    }

    val tNone = new StrainerCapsule(EmptyTask("None"))
    val tNone2 = new StrainerCapsule(EmptyTask("None"))

    val strainer = EmptyTask("Strainer")

    val t2 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(p)
      override def process(context: Context) = {
        context.value(p).get should equal("Test")
        context
      }
    }

    val strainerC = Slot(new StrainerCapsule(strainer))

    val ex = (root -- tNone -- (t1, tNone2) -- strainerC -- t2) + (root oo strainerC)
    ex.start.waitUntilEnded
  }

}
