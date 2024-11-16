/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.workflow.mole

import org.openmole.core.context.Val
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.task.EmptyTask
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.setter._
import org.scalatest._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.test.TestTask

class StrainerCapsuleSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "The strainer capsule" should "let the data pass through" in {
    val p = Val[String]

    val t1 = TestTask { _ + (p -> "Test") } set (outputs += p)
    val strainer = EmptyTask()

    val t2 = TestTask { context =>
      context(p) should equal("Test")
      context
    } set (inputs += p)

    val ex = t1 -- Strain(strainer) -- t2
    ex.run
  }

  "The strainer capsule" should "let the data pass through even if linked with a data channel to the root" in {
    @volatile var executed = false
    val p = Val[String]

    val root = EmptyTask()

    val t1 = TestTask { _ + (p -> "Test") } set (outputs += p)

    val tNone = EmptyTask()
    val tNone2 = EmptyTask()

    val strainer = EmptyTask()

    val t2 = TestTask { context =>
      context(p) should equal("Test")
      executed = true
      context
    } set (inputs += p)

    val ex = (Strain(root) -- Strain(tNone) -- (t1, Strain(tNone2)) -- Strain(strainer) -- t2) & (root oo strainer)
    ex.run
    executed should equal(true)
  }

}
