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

package org.openmole.core.workflow.puzzle

import org.openmole.core.context.Val
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.test.TestTask
import org.scalatest._

class EndExplorationTransitionSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "EndExploration transition" should "kill the submole when triggered" in {
    @volatile var endCapsExecuted = 0

    val data = List("A", "A", "B", "C")
    val i = Val[String]("i")

    val sampling = ExplicitSampling(i, data)

    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val testT = TestTask { context =>
      context.contains(i) should equal(true)
      endCapsExecuted += 1
      context
    } set (inputs += i)

    val ex = ExplorationTask(sampling) -< (emptyT >| testT when "true")

    ex.run
    endCapsExecuted should equal(1)
  }

}
