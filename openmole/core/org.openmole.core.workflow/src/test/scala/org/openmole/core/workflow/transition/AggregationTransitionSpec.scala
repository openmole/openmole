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

package org.openmole.core.workflow.transition

import java.util.concurrent.atomic.AtomicInteger

import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._

import org.scalatest._
import scala.collection.mutable.ListBuffer
import scala.reflect.macros.whitebox
import scala.util.Try

class AggregationTransitionSpec extends FlatSpec with Matchers {

  implicit val plugins = PluginSet.empty

  "Aggregation transition" should "turn results of exploration into a array of values" in {
    @volatile var endCapsExecuted = 0

    val data = List("A", "A", "B", "C")
    val i = Prototype[String]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val emptyT = EmptyTask()
    emptyT addInput i
    emptyT addOutput i

    val emptyC = Capsule(emptyT)

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

    val testC = Capsule(testT)

    val mole = exc -< emptyC >- testC toMole

    MoleExecution(mole).start.waitUntilEnded
    endCapsExecuted should equal(1)
    MoleExecution(mole).start.waitUntilEnded
    endCapsExecuted should equal(2)
  }

  "Aggregation transition" should "should also work for native types" in {
    @volatile var endCapsExecuted = 0

    val data = List(1, 2, 3, 2)
    val i = Prototype[Int]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val emptyT = EmptyTask()
    emptyT addInput i
    emptyT addOutput i

    val emptyC = Capsule(emptyT)

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

    val testC = Capsule(testT)

    val ex = exc -< emptyC >- testC

    ex.start.waitUntilEnded
    endCapsExecuted should equal(1)
  }

  "Aggregation transition" should "support cancel and start of a new execution" in {
    val endCapsExecuted = new AtomicInteger()

    val data = 0 to 1000
    val i = Prototype[Int]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val emptyT = EmptyTask()
    emptyT addInput i
    emptyT addOutput i

    val emptyC = Capsule(emptyT)

    val testT = new TestTask {
      val name = "Test"
      override val inputs = DataSet(i.toArray)
      override def process(context: Context) = {
        context.contains(i.toArray) should equal(true)
        context(i.toArray).sorted.deep should equal(data.toArray.deep)
        endCapsExecuted.incrementAndGet()
        context
      }
    }

    val testC = Capsule(testT)

    val mole = exc -< emptyC >- testC toMole

    MoleExecution(mole).start.cancel
    endCapsExecuted.set(0)
    MoleExecution(mole).start.waitUntilEnded
    endCapsExecuted.get() should equal(1)
  }

  "Aggregation transition" should "not be executed when a task failed in exploration" in {
    val data = 0 to 1000
    val i = Prototype[Int]("i")
    val sampling = new ExplicitSampling(i, data)
    val exploration = ExplorationTask(sampling)
    val endCapsExecuted = new AtomicInteger()

    val run = new TestTask {
      val name = "Run"
      override val inputs = DataSet(i)
      override val outputs = DataSet(i)
      override def process(context: Context) = {
        if (context(i) == 42) throw new InternalProcessingError("Some error for test")
        context
      }
    }

    val test = new TestTask {
      val name = "Test"
      override val inputs = DataSet(i.toArray)
      override def process(context: Context) = {
        endCapsExecuted.incrementAndGet()
        context
      }
    }

    val ex = (exploration -< run >- test).start
    Try { ex.waitUntilEnded }

    endCapsExecuted.get() should equal(0)
  }

}

