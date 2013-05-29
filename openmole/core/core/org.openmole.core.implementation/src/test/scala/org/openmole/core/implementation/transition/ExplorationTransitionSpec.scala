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

package org.openmole.core.implementation.transition

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.sampling._
import org.openmole.core.model.data._
import org.openmole.core.model.sampling._
import org.openmole.core.model.task._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class ExplorationTransitionSpec extends FlatSpec with ShouldMatchers {

  implicit val plugins = PluginSet.empty

  "Exploration transition" should "submit one MoleJob for each value in the sampling" in {

    val data = List("A", "B", "C")
    val i = Prototype[String]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val res = new ListBuffer[String]

    val t = new TestTask {
      val name = "Test"
      override def inputs = DataSet(i)
      override def process(context: Context) = synchronized {
        context.contains(i) should equal(true)
        res += context.value(i).get
        context
      }
    }

    val ex = exc -< t
    ex.start.waitUntilEnded
    res.toArray.sorted.deep should equal(data.toArray.deep)
  }

  "Exploration transition" should "work with the DSL interface" in {

    println("Test")

    val data = List("A", "B", "C")
    val i = Prototype[String]("i")

    val explo = ExplorationTask("Exploration", new ExplicitSampling(i, data))

    val res = new ListBuffer[String]

    val t = new TestTask {
      val name = "Test"
      override def inputs = DataSet(i)
      override def process(context: Context) = synchronized {
        context.contains(i) should equal(true)
        res += context(i)
        context
      }
    }

    (explo -< t).start.waitUntilEnded
    res.toArray.sorted.deep should equal(data.toArray.deep)
  }
}
