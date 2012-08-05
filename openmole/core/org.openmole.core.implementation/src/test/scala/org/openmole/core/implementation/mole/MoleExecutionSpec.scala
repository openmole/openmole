/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.core.implementation.mole

import org.openmole.core.implementation.task.TestTask
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.sampling.ExplicitSampling
import org.openmole.core.model.data.IContext
import org.openmole.core.model.mole.IMoleJobGroup
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IGrouping
import org.openmole.core.model.sampling.ISampling
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class MoleExecutionSpec extends FlatSpec with ShouldMatchers {

  implicit val plugins = PluginSet.empty

  class JobGroupingBy2Test extends IGrouping {

    def apply(context: IContext, groups: Iterable[(IMoleJobGroup, Iterable[IMoleJob])]): IMoleJobGroup = {
      groups.find { case (_, g) ⇒ g.size < 2 } match {
        case Some((mg, _)) ⇒ mg
        case None ⇒ MoleJobGroup()
      }
    }

  }

  "Grouping jobs" should "not impact a normal mole execution" in {
    val data = List("A", "A", "B", "C")
    val i = new Prototype[String]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = new Capsule(ExplorationTask("Exploration", sampling))

    val emptyT = EmptyTask("Empty")
    emptyT.addInput(i)
    emptyT.addOutput(i)

    val emptyC = new Capsule(emptyT)

    val testT = new TestTask {
      val name = "Test"
      override val inputs = DataSet(i.toArray)
      override def process(context: IContext) = {
        context.contains(i.toArray) should equal(true)
        context.value(i.toArray).get.sorted.deep should equal(data.toArray.deep)
        context
      }
    }

    val testC = new Capsule(testT)

    new ExplorationTransition(exc, emptyC)
    new AggregationTransition(emptyC, testC)

    new MoleExecution(
      mole = new Mole(exc),
      grouping = Map(emptyC -> new JobGroupingBy2Test)).start.waitUntilEnded
  }
}
