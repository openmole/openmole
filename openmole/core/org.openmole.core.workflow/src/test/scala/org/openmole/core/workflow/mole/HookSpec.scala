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
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.scalatest._
import org.scalatest.junit._

class HookSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.tools.StubServices._

  "A hook" should "intercept the execution of a capsule" in {
    val executed = new AtomicInteger(0)

    val p = Val[String]("p")

    val t1 =
      TestTask { _ + (p → "test") } set (
        name := "Test",
        outputs += p
      )

    val t1c = Capsule(t1)

    val hook = TestHook { context ⇒
      context.contains(p) should equal(true)
      context(p) should equal("test")
      executed.incrementAndGet()
      context
    }

    val ex = t1c hook hook

    ex.run

    executed.get should equal(1)
  }

  "A hook" should "intercept the execution of a master capsule" in {
    @transient var executed = false

    val p = Val[String]("p")

    val t1 =
      TestTask { _ + (p → "test") } set (
        name := "Test",
        outputs += p
      )

    val t1c = MasterCapsule(t1)

    val hook = TestHook { context ⇒
      context.contains(p) should equal(true)
      context(p) should equal("test")
      executed = true
      context
    }

    val ex = MoleExecution(Mole(t1c), hooks = List(t1c → hook))

    ex.run

    executed should equal(true)
  }

}
