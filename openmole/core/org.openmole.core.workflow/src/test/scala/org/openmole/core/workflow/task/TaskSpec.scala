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

class TaskSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  import org.openmole.core.workflow.test.Stubs.*

  "Task" should "capture name" in:
    val myTask = EmptyTask()
    myTask.name should equal(Some("myTask"))