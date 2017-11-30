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

import org.openmole.core.context.Val
import org.openmole.core.exception._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.scalatest._

import scala.collection.mutable.ListBuffer
import scala.reflect.macros.whitebox
import scala.util.Try

class AggregationTransitionSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.tools.StubServices._

  "Aggregation transition" should "turn results of exploration into a array of values" in {
    @volatile var endCapsExecuted = 0

    val data = List("A", "A", "B", "C")
    val i = Val[String]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val emptyC = Capsule(emptyT)

    val testT = TestTask { context ⇒
      context.contains(i.toArray) should equal(true)
      context(i.toArray).sorted.deep should equal(data.toArray.deep)
      endCapsExecuted += 1
      context
    } set (
      name := "Test",
      inputs += i.array
    )

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
    val i = Val[Int]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val emptyT = EmptyTask() set ((inputs, outputs) += i)
    val emptyC = Capsule(emptyT)

    val testT = TestTask { context ⇒
      context.contains(i.toArray) should equal(true)
      context(i.toArray).getClass should equal(classOf[Array[Int]])
      context(i.toArray).sorted.deep should equal(data.sorted.toArray.deep)
      endCapsExecuted += 1
      context
    } set (
      name := "Test",
      inputs += i.array
    )

    val testC = Capsule(testT)

    val ex = exc -< emptyC >- testC

    ex.start.waitUntilEnded
    endCapsExecuted should equal(1)
  }

  "Aggregation transition" should "support cancel and start of a new execution" in {
    val endCapsExecuted = new AtomicInteger()

    val data = 0 to 1000
    val i = Val[Int]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val emptyC = Capsule(emptyT)

    val testT = TestTask { context ⇒
      context.contains(i.toArray) should equal(true)
      context(i.toArray).sorted.deep should equal(data.toArray.deep)
      endCapsExecuted.incrementAndGet()
      context
    } set (
      name := "Test",
      inputs += i.array
    )

    val testC = Capsule(testT)
    val mole = exc -< emptyC >- testC toMole

    MoleExecution(mole).start.cancel
    endCapsExecuted.set(0)
    MoleExecution(mole).start.waitUntilEnded
    endCapsExecuted.get() should equal(1)
  }

  "Aggregation transition" should "not be executed when a task failed in exploration" in {
    val data = 0 to 1000
    val i = Val[Int]("i")
    val sampling = new ExplicitSampling(i, data)
    val exploration = ExplorationTask(sampling)
    val endCapsExecuted = new AtomicInteger()

    val run =
      TestTask { context ⇒
        if (context(i) == 42) throw new InternalProcessingError("Some error for test")
        context
      } set (
        name := "Run",
        (inputs, outputs) += i
      )

    val test = TestTask { context ⇒
      endCapsExecuted.incrementAndGet()
      context
    } set (
      name := "Test",
      inputs += i.array
    )

    val ex = (exploration -< run >- test).start
    Try { ex.waitUntilEnded }

    endCapsExecuted.get() should equal(0)
  }

  "Multiple aggregation transition" should "all be executed" in {
    val v = Val[Double]("v")
    val m = Val[Double]("m")
    val s = Val[Double]("s")

    val executed = new AtomicInteger()

    def exploration = ExplorationTask(ExplicitSampling(v, 0.0 until 10.0 by 1.0))

    def main =
      EmptyTask() set (
        name := "main",
        (inputs, outputs) += v
      )

    def test =
      TestTask { ctx ⇒ executed.incrementAndGet(); ctx } set (
        name := "mean",
        inputs += v.array
      )

    val ex = exploration -< main >- (test, test) start

    ex.waitUntilEnded
    executed.get should equal(2)
  }

  "Order" should "be preserved" in {
    val v = Val[Double]("v")
    val s = Val[Double]("s")

    val executed = new AtomicInteger()

    def exploration = ExplorationTask(ExplicitSampling(v, 0.0 until 100.0 by 1.0))

    def main =
      EmptyTask() set (
        name := "main",
        (inputs, outputs) += v
      )

    def test =
      TestTask { ctx ⇒ executed.incrementAndGet(); ctx(v.toArray).toSeq.sorted should equal(ctx(v.toArray).toSeq); ctx } set (
        name := "mean",
        inputs += v.array
      )

    val env = LocalEnvironment(100)

    val ex = exploration -< (main on env) >- test start

    ex.waitUntilEnded
    executed.get should equal(1)
  }

}

