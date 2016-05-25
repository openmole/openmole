/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.task.tools

import monocle.macros.Lenses
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.dsl
import dsl._

object AssignTask {

  implicit def isBuilder = TaskBuilder[AssignTask].from(this)

  def apply(assignments: (Prototype[T], Prototype[T]) forSome { type T }*): AssignTask =
    new AssignTask(assignments.toVector) set (
      dsl.inputs += (assignments.map(_._1): _*),
      dsl.outputs += (assignments.map(_._2): _*)
    )

}
@Lenses case class AssignTask(
    assignments: Vector[(Prototype[T], Prototype[T]) forSome { type T }],
    inputs:      PrototypeSet                                            = PrototypeSet.empty,
    outputs:     PrototypeSet                                            = PrototypeSet.empty,
    defaults:    DefaultSet                                              = DefaultSet.empty,
    name:        Option[String]                                          = None
) extends Task {

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) =
    assignments.map { case (from, to) ⇒ Variable(to, context(from)) }

}
