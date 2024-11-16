/*
 * Copyright (C) 16/02/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.task

import org.openmole.core.context.Val
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.setter.*
import org.openmole.core.workflow.dsl.*
import org.openmole.core.workflow.mole.*
import org.openmole.core.workflow.puzzle.*
import org.openmole.core.workflow.sampling.ExplicitSampling
import org.openmole.core.workflow.test.TestTask
import org.openmole.core.workflow.transition.*
import org.scalatest.*

import scala.util.Try

class TryTaskSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  import org.openmole.core.workflow.test.Stubs.*

  "Try task" should "run" in:
    val i = Val[String]
    val o = Val[Int]

    val emptyT =
      TestTask: context =>
        context + (o -> 9)
      .set (inputs += i, i := "test", outputs += o)

    val testT =
      TestTask: context =>
        assert(context(o) == 9)
        context
      .set (inputs += o)

    val tryTask = TryTask (emptyT) set (o := 10)
    (tryTask -- testT) run


  "Try task" should "produce a result in case of failing inner task" in :
    val i = Val[String]
    val o = Val[Int]

    val emptyT =
      TestTask: context =>
        throw new RuntimeException("error in task")
      .set(inputs += i, i := "test", outputs += o)

    val testT =
      TestTask: context =>
        assert(context(o) == 10)
        context
      .set(inputs += o)

    val tryTask = TryTask(emptyT) set (o := 10)
    (tryTask -- testT) run