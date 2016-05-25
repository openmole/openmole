/*
 * Copyright (C) 28/11/12 Romain Reuillon
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

import monocle.macros.Lenses
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.dsl
import dsl._

import reflect.ClassTag

object MergeTask {

  implicit def isBuilder[S] = new TaskBuilder[MergeTask[S]] {
    override def name = MergeTask.name[S]
    override def outputs = MergeTask.outputs[S]
    override def inputs = MergeTask.inputs[S]
    override def defaults = MergeTask.defaults[S]
  }

  def apply[S](result: Prototype[Array[S]], prototypes: Prototype[Array[S]]*) =
    new MergeTask[S](
      result,
      prototypes.toVector,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None
    ) set (
      dsl.inputs += (prototypes: _*),
      dsl.outputs += result
    )

}

@Lenses case class MergeTask[S](
    result:     Prototype[Array[S]],
    prototypes: Vector[Prototype[Array[S]]],
    inputs:     PrototypeSet,
    outputs:    PrototypeSet,
    defaults:   DefaultSet,
    name:       Option[String]
) extends Task {

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
    val flattened = prototypes.map { p ⇒ context(p) }.flatten.toArray[S](ClassTag(result.fromArray.`type`.runtimeClass))
    Variable(result, flattened)
  }

}
