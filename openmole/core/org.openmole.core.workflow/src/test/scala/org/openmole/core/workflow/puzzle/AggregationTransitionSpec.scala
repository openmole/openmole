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

import java.util.concurrent.atomic.AtomicInteger

import org.openmole.core.context.Val
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.execution.LocalEnvironment
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.scalatest._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.core.workflow.test.TestTask
import org.openmole.core.workflow.transition.TransitionSlot
import org.openmole.core.workflow.validation.Validation

import scala.util.Try

class AggregationTransitionSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test._

  "Aggregation transition" should "turn results of exploration into a array of values" in {

    try {
      @volatile var endCapsExecuted = 0

      val data = List("A", "A", "B", "C")
      val i = Val[String]

      val emptyT = EmptyTask() set ((inputs, outputs) += i)

      val testT = TestTask { context =>
        context.contains(i.toArray) should equal(true)
        context(i.toArray).sorted.toVector should equal(data.toVector)
        endCapsExecuted += 1
        context
      } set (inputs += i.array)

      val mole = ExplicitSampling(i, data) -< emptyT >- testT

      mole.run
      endCapsExecuted should equal(1)

      mole.run
      endCapsExecuted should equal(2)
    }
    catch {
      case e: Throwable => e.printStackTrace
    }
  }

  "Aggregation transition" should "also work for native types" in {
    @volatile var endCapsExecuted = 0

    val data = List(1, 2, 3, 2)
    val i = Val[Int]

    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val testT =
      TestTask { context =>
        context.contains(i.toArray) should equal(true)
        context(i.toArray).getClass should equal(classOf[Array[Int]])
        context(i.toArray).sorted.toVector should equal(data.sorted.toVector)
        endCapsExecuted += 1
        context
      } set (inputs += i.array)

    val ex = ExplicitSampling(i, data) -< emptyT >- testT

    ex.run
    endCapsExecuted should equal(1)
  }

  "Aggregation transition" should "support cancel and start of a new execution" in {
    val endCapsExecuted = new AtomicInteger()

    val data = 0 to 1000
    val i = Val[Int]

    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val testT =
      TestTask { context =>
        context.contains(i.toArray) should equal(true)
        context(i.toArray).sorted.toVector should equal(data.toVector)
        endCapsExecuted.incrementAndGet()
        context
      } set (inputs += i.array)

    val mole = ExplicitSampling(i, data) -< emptyT >- testT

    mole.start(false).cancel
    endCapsExecuted.set(0)
    mole.run
    endCapsExecuted.get() should equal(1)
  }

  "Aggregation transition" should "not be executed when a task failed in exploration" in {
    val data = 0 to 1000
    val i = Val[Int]

    val endCapsExecuted = new AtomicInteger()

    val run =
      TestTask { context =>
        if (context(i) == 42) throw new InternalProcessingError("Some error for test")
        context
      } set ((inputs, outputs) += i)

    val test = TestTask { context =>
      endCapsExecuted.incrementAndGet()
      context
    } set (inputs += i.array)

    Try { (ExplicitSampling(i, data) -< run >- test).run }

    endCapsExecuted.get() should equal(0)
  }

  "Multiple aggregation transition" should "all be executed" in {
    val v = Val[Double]("v")
    val m = Val[Double]("m")
    val s = Val[Double]("s")

    val executed = new AtomicInteger()

    def exploration = ExplorationTask(ExplicitSampling(v, (BigDecimal(0.0) until 10.0 by 1.0).map(_.toDouble)))

    def main =
      EmptyTask() set (
        name := "main",
        (inputs, outputs) += v
      )

    def test =
      TestTask { ctx => executed.incrementAndGet(); ctx } set (
        name := "mean",
        inputs += v.array
      )

    val ex = (exploration -< main >- (test, test))

    ex.run

    executed.get should equal(2)
  }

  "Order" should "be preserved" in {
    def test = {
      val v = Val[Double]
      val s = Val[Double]

      val executed = new AtomicInteger()

      val sampling = BigDecimal(0.0) to 100.0 by 1.0 map (_.toDouble)

      def main =
        EmptyTask() set ((inputs, outputs) += v)

      def test =
        TestTask {
          ctx =>
            executed.incrementAndGet()
            ctx(v.toArray).toSeq should equal(sampling)
            ctx
        } set (inputs += v.array)

      val env = LocalEnvironment(10)

      ExplicitSampling(v, sampling) -< (main on env) >- test run

      executed.get should equal(1)
    }

    for (i <- 0 until 10) test
  }

}

