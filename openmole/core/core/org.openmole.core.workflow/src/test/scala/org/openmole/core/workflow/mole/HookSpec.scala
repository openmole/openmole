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

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole._

import org.scalatest._
import org.scalatest.junit._

class HookSpec extends FlatSpec with Matchers {

  "A capsule execution misc" should "intercept the execution of a capsule" in {
    var executed = false

    val p = Prototype[String]("p")

    val t1 = new TestTask {
      val name = "Test"
      override val outputs = DataSet(p)
      override def process(context: Context) = context + (p -> "test")
    }

    val t1c = Capsule(t1)

    val hook = new HookBuilder {
      def toHook = new Hook with Built {
        override def process(context: Context, executionContext: ExecutionContext) = {
          context.contains(p) should equal(true)
          context(p) should equal("test")
          executed = true
          context
        }
      }
    }

    val ex = MoleExecution(Mole(t1c), hooks = List(t1c -> hook))

    ex.start.waitUntilEnded

    executed should equal(true)
  }

  "A capsule execution misc" should "intercept the execution of a master capsule" in {
    var executed = false

    val p = Prototype[String]("p")

    val t1 = new TestTask {
      val name = "Test"
      override val outputs = DataSet(p)
      override def process(context: Context) = context + (p -> "test")

    }

    val t1c = MasterCapsule(t1)

    val hook = new HookBuilder {
      def toHook = new Hook with Built {
        override def process(context: Context, executionContext: ExecutionContext) = {
          context.contains(p) should equal(true)
          context(p) should equal("test")
          executed = true
          context
        }
      }
    }

    val ex = MoleExecution(Mole(t1c), hooks = List(t1c -> hook))

    ex.start.waitUntilEnded

    executed should equal(true)
  }

}
