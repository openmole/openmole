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

package org.openmole.core.workflow.hook

import java.util.concurrent.atomic.AtomicInteger

import org.openmole.core.context.Val
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole._
import org.openmole.core.setter._
import org.openmole.core.workflow.hook.*
import org.openmole.core.workflow.composition.TaskNode
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.test.{ TestHook, TestTask }
import org.scalatest._

class HookSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.*


  "A hook" should "intercept the execution of a task" in {
    val executed = new AtomicInteger(0)
    val p = Val[String]

    val t1 =
      TestTask { _ + (p -> "test") } set (
        name := "Test",
        outputs += p
      )

    val hook = TestHook { context =>
      context.contains(p) should equal(true)
      context(p) should equal("test")
      executed.incrementAndGet()
    }

    val ex = t1 hook hook

    ex.run

    executed.get should equal(1)
  }

  "A hook" should "intercept the execution of a master capsule" in {
    @transient var executed = false

    val p = Val[String]("p")

    val t1 =
      TestTask { _ + (p -> "test") } set (
        outputs += p
      )

    val hook = TestHook { context =>
      context.contains(p) should equal(true)
      context(p) should equal("test")
      executed = true
    }

    val ex = Master(t1) hook hook

    ex.run

    executed should equal(true)
  }

  "Display hook" should "be accepted" in {
    val t1 = EmptyTask()
    val ex = t1 hook display
    (ex: DSL)
  }

}
