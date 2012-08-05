/*
 * Copyright (C) 2011 reuillon
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

import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.sampling.ExplicitSampling
import org.openmole.core.model.data.IContext
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class DataChannelSpec extends FlatSpec with ShouldMatchers {

  implicit val plugins = PluginSet.empty

  "A datachannel" should "enable variable values to be transmitted from a task to another" in {
    val p = new Prototype[String]("p")

    val t1 =
      new TestTask {
        val name = "Test write"
        override val outputs = DataSet(p)
        override def process(context: IContext) = context + (p -> "Test")
      }

    val t2 = EmptyTask("Inter task")

    val t3 = new TestTask {
      val name = "Test read"
      override val inputs = DataSet(p)
      override def process(context: IContext) = {
        context.value(p).get should equal("Test")
        context
      }
    }

    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    val t3c = new Capsule(t3)

    new Transition(t1c, t2c)
    new Transition(t2c, t3c)

    new DataChannel(t1c, t3c)

    new MoleExecution(new Mole(t1c)).start.waitUntilEnded
  }

  "A data channel" should "be able to transmit the value to the multiple execution of an explored task" in {

    val j = new Prototype[String]("j")
    val tw = new TestTask {
      val name = "Test write"
      override def outputs = DataSet(j)
      override def process(context: IContext) = context + (j -> "J")
    }

    val data = List("A", "B", "C")
    val i = new Prototype[String]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val res = new ListBuffer[String]

    val t = new TestTask {
      val name = "Test"
      override val inputs = DataSet(i, j)
      override def process(context: IContext) = synchronized {
        context.contains(i) should equal(true)
        context.contains(j) should equal(true)
        res += context.value(i).get
        context
      }
    }

    val twc = new Capsule(tw)
    val tc = new Capsule(t)

    new DataChannel(twc, tc)

    new Transition(twc, exc)
    new ExplorationTransition(exc, tc)
    new MoleExecution(new Mole(twc)).start.waitUntilEnded
    res.toArray.sorted.deep should equal(data.toArray.deep)
  }

  "A data channel" should "be able to gather the values of the multiple execution of an explored task" in {
    var executed = false
    val data = List("A", "B", "C")
    val i = new Prototype[String]("i")
    val j = new Prototype[String]("j")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val t = new TestTask {
      val name = "Test"
      override val outputs = DataSet(j)
      override def process(context: IContext) = context + (j -> "J")
    }

    val noOP = EmptyTask("NoOP")

    val tr = new TestTask {
      val name = "Test read"
      override val inputs = DataSet(j.toArray)
      override def process(context: IContext) = {
        context.value(j.toArray).get.size should equal(data.size)
        executed = true
        context
      }
    }

    val tc = new Capsule(t)
    val noOPC = new Capsule(noOP)
    val trc = new Capsule(tr)

    new DataChannel(tc, trc)

    val ex = exc -< tc -- noOPC >- trc toExecution

    ex.start.waitUntilEnded
    executed should equal(true)
  }
}
