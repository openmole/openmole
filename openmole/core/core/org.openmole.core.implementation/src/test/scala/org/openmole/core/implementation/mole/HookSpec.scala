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

package org.openmole.core.implementation.hook

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.mole._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class HookSpec extends FlatSpec with ShouldMatchers {

  "A capsule execution misc" should "intercept the execution of a capsule" in {
    var executed = false

    val p = Prototype[String]("p")

    val t1 = new TestTask {
      val name = "Test"
      override val outputs = DataSet(p)
      override def process(context: Context) = context + (p -> "test")
    }

    val t1c = new Capsule(t1)

    val hook = new HookBuilder {
      def toHook = new IHook with Built {
        override def perform(context: Context, executionContext: ExecutionContext) = {
          context.contains(p) should equal(true)
          context(p) should equal("test")
          executed = true
          context
        }
      }
    }

    val ex = MoleExecution(new Mole(t1c), hooks = List(t1c -> hook))

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

    val t1c = new MasterCapsule(t1)

    val hook = new HookBuilder {
      def toHook = new IHook with Built {
        override def perform(context: Context, executionContext: ExecutionContext) = {
          context.contains(p) should equal(true)
          context(p) should equal("test")
          executed = true
          context
        }
      }
    }

    val ex = MoleExecution(new Mole(t1c), hooks = List(t1c -> hook))

    ex.start.waitUntilEnded

    executed should equal(true)
  }

}
