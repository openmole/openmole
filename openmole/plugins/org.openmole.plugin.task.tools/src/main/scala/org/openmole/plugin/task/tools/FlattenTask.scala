/*
 * Copyright (C) 03/09/13 Romain Reuillon
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

package org.openmole.plugin.task.tools

import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._

import scala.reflect.ClassTag
import org.openmole.core.dsl
import dsl._
import monocle.macros.Lenses

object FlattenTask {

  implicit def isBuilder[S]: TaskBuilder[FlattenTask[S]] = new TaskBuilder[FlattenTask[S]] {
    override def name = FlattenTask.name[S]
    override def outputs = FlattenTask.outputs[S]
    override def inputs = FlattenTask.inputs[S]
    override def defaults = FlattenTask.defaults[S]
  }

  def apply[S](flatten: Prototype[Array[Array[S]]], in: Prototype[Array[S]]) =
    new FlattenTask(flatten, in) set (
      dsl.inputs += flatten,
      dsl.outputs += in
    )

}

@Lenses case class FlattenTask[S](
    flatten:  Prototype[Array[Array[S]]],
    in:       Prototype[Array[S]],
    inputs:   PrototypeSet               = PrototypeSet.empty,
    outputs:  PrototypeSet               = PrototypeSet.empty,
    defaults: DefaultSet                 = DefaultSet.empty,
    name:     Option[String]             = None
) extends Task {

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
    implicit val sClassTag = ClassTag[S](in.fromArray.`type`.runtimeClass)
    Variable(in, context(flatten).flatten.toArray[S])
  }

}
