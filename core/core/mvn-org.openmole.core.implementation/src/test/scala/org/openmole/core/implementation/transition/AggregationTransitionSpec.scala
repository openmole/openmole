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

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.sampling.ExplicitSampling
import org.openmole.core.model.transition._
import org.openmole.core.model.data._
import org.openmole.core.model.sampling._
import org.openmole.core.model.task._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class AggregationTransitionSpec extends FlatSpec with ShouldMatchers {

  implicit val plugins = PluginSet.empty

  "Aggregation transition" should "turn results of exploration into a array of values" in {
    @volatile var endCapsExecuted = 0

    val data = List("A", "A", "B", "C")
    val i = Prototype[String]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val emptyT = EmptyTask("Empty")
    emptyT addInput i
    emptyT addOutput i

    val emptyC = new Capsule(emptyT)

    val testT = new TestTask {
      val name = "Test"
      override def inputs = DataSet(i.toArray)
      override def process(context: Context) = {
        context.contains(i.toArray) should equal(true)
        context(i.toArray).sorted.deep should equal(data.toArray.deep)
        endCapsExecuted += 1
        context
      }
    }

    val testC = new Capsule(testT)

    val mole = exc -< emptyC >- testC toMole

    new MoleExecution(mole).start.waitUntilEnded
    endCapsExecuted should equal(1)
    new MoleExecution(mole).start.waitUntilEnded
    endCapsExecuted should equal(2)
  }

  "Aggregation transition" should "should also work for native types" in {
    @volatile var endCapsExecuted = 0

    val data = List(1, 2, 3, 2)
    val i = Prototype[Int]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val emptyT = EmptyTask("Empty")
    emptyT addInput i
    emptyT addOutput i

    val emptyC = new Capsule(emptyT)

    val testT = new TestTask {
      val name = "Test"
      override val inputs = DataSet(i.toArray)
      override def process(context: Context) = {
        context.contains(i.toArray) should equal(true)

        context(i.toArray).getClass should equal(classOf[Array[Int]])
        context(i.toArray).sorted.deep should equal(data.sorted.toArray.deep)
        endCapsExecuted += 1
        context
      }
    }

    val testC = new Capsule(testT)

    val ex = exc -< emptyC >- testC

    ex.start.waitUntilEnded
    endCapsExecuted should equal(1)
  }

  "Aggregation transition" should "support cancel and start of a new execution" in {
    @volatile var endCapsExecuted = 0

    val data = 0 to 1000
    val i = Prototype[Int]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val emptyT = EmptyTask("Empty")
    emptyT addInput i
    emptyT addOutput i

    val emptyC = new Capsule(emptyT)

    val testT = new TestTask {
      val name = "Test"
      override val inputs = DataSet(i.toArray)
      override def process(context: Context) = {
        context.contains(i.toArray) should equal(true)
        context(i.toArray).sorted.deep should equal(data.toArray.deep)
        endCapsExecuted += 1
        context
      }
    }

    val testC = new Capsule(testT)

    val mole = exc -< emptyC >- testC toMole

    new MoleExecution(mole).start.cancel
    endCapsExecuted = 0
    new MoleExecution(mole).start.waitUntilEnded
    endCapsExecuted should equal(1)
  }

}

