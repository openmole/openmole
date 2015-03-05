/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.workflow.transition

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.scalatest._
import scala.collection.mutable.ListBuffer

class EndExplorationTransitionSpec extends FlatSpec with Matchers {

  implicit val plugins = PluginSet.empty

  "EndExploration transition" should "kill the submole when triggered" in {
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
      override def inputs = DataSet(i)
      override def process(context: Context) = {
        context.contains(i) should equal(true)
        endCapsExecuted += 1
        context
      }
    }

    val testC = Capsule(testT)

    val ex = exc -< emptyC >| (testC, "true")

    ex.start.waitUntilEnded
    endCapsExecuted should equal(1)
  }

}
