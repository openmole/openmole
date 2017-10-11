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

import org.openmole.core.context.Val
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.builder._
import org.scalatest._

import scala.collection.mutable.ListBuffer
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.dsl._

class EndExplorationTransitionSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.Services._

  "EndExploration transition" should "kill the submole when triggered" in {
    @volatile var endCapsExecuted = 0

    val data = List("A", "A", "B", "C")
    val i = Val[String]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val emptyC = Capsule(emptyT)

    val testT = TestTask { context ⇒
      context.contains(i) should equal(true)
      endCapsExecuted += 1
      context
    } set (
      name := "Test",
      inputs += i
    )

    val testC = Capsule(testT)

    val ex = exc -< emptyC >| (testC when "true")

    ex.start.waitUntilEnded
    endCapsExecuted should equal(1)
  }

}
