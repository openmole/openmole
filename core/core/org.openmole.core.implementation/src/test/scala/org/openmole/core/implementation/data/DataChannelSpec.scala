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

package org.openmole.core.implementation.data

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.sampling._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class DataChannelSpec extends FlatSpec with ShouldMatchers {

  implicit val plugins = PluginSet.empty

  "A datachannel" should "enable variable values to be transmitted from a task to another" in {
    val p = Prototype[String]("p")

    val t1 =
      new TestTask {
        val name = "Test write"
        override val outputs = DataSet(p)
        override def process(context: Context) = context + (p -> "Test")
      }

    val t2 = EmptyTask("Inter task")

    val t3 = new TestTask {
      val name = "Test read"
      override val inputs = DataSet(p)
      override def process(context: Context) = {
        context(p) should equal("Test")
        context
      }
    }

    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    val t3c = Slot(new Capsule(t3))

    val ex = (t1c -- t2c -- t3c) + (t1c oo t3c)

    ex.start.waitUntilEnded
  }

  "A data channel" should "be able to transmit the value to the multiple execution of an explored task" in {

    val j = Prototype[String]("j")
    val tw = new TestTask {
      val name = "Test write"
      override def outputs = DataSet(j)
      override def process(context: Context) = context + (j -> "J")
    }

    val data = List("A", "B", "C")
    val i = Prototype[String]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val res = new ListBuffer[String]

    val t = new TestTask {
      val name = "Test"
      override val inputs = DataSet(i, j)
      override def process(context: Context) = synchronized {
        context.contains(i) should equal(true)
        context.contains(j) should equal(true)
        res += context(i)
        context
      }
    }

    val twc = Capsule(tw)
    val tc = Slot(t)

    val ex = (twc -- exc -< tc) + (twc oo tc)
    ex.start.waitUntilEnded
    res.toArray.sorted.deep should equal(data.toArray.deep)
  }

  "A data channel" should "be able to gather the values of the multiple execution of an explored task" in {
    var executed = false
    val data = List("A", "B", "C")
    val i = Prototype[String]("i")
    val j = Prototype[String]("j")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask("Exploration", sampling))

    val t = new TestTask {
      val name = "Test"
      override val outputs = DataSet(j)
      override def process(context: Context) = context + (j -> "J")
    }

    val noOP = EmptyTask("NoOP")

    val tr = new TestTask {
      val name = "Test read"
      override val inputs = DataSet(j.toArray)
      override def process(context: Context) = {
        context(j.toArray).size should equal(data.size)
        executed = true
        context
      }
    }

    val tc = Capsule(t)
    val noOPC = Capsule(noOP)
    val trc = Slot(tr)

    val ex = (exc -< tc -- noOPC >- trc) + (tc oo trc)

    ex.start.waitUntilEnded
    executed should equal(true)
  }
}
