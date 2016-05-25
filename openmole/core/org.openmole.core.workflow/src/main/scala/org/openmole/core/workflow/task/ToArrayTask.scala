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

package org.openmole.core.workflow.task

import monocle._
import monocle.macros.Lenses
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.builder
import builder._
import org.openmole.core.workflow.dsl
import dsl._

import scala.reflect.ClassTag

object ToArrayTask {

  implicit def isBuilder = new TaskBuilder[ToArrayTask] {
    override def defaults = ToArrayTask.defaults
    override def inputs = ToArrayTask.inputs
    override def name = ToArrayTask.name
    override def outputs = ToArrayTask.outputs
  }

  def apply(prototypes: Prototype[T] forSome { type T }*) = {
    val t = new ToArrayTask(prototypes)

    t set (
      dsl.inputs += (prototypes: _*),
      dsl.outputs += (prototypes.map(_.array): _*)
    )
  }

}

@Lenses case class ToArrayTask(
    prototypes: Seq[Prototype[T] forSome { type T }],
    inputs:     PrototypeSet                         = PrototypeSet.empty,
    outputs:    PrototypeSet                         = PrototypeSet.empty,
    defaults:   DefaultSet                           = DefaultSet.empty,
    name:       Option[String]                       = None
) extends Task {

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) =
    prototypes.map {
      p ⇒ Variable.unsecure(p.toArray, Array(context(p))(ClassTag(p.`type`.runtimeClass)))
    }

}
