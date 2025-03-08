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

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.dsl
import org.openmole.core.dsl._
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.task._

import scala.reflect.ClassTag

object MergeTask:

  def apply[S](result: Val[Array[S]], prototypes: Val[Array[S]]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("MergeTask"): p =>
      import p.*
      val flattened = prototypes.map { p => context(p) }.flatten.toArray[S](ClassTag(result.fromArray.`type`.runtimeClass))
      Variable(result, flattened)
    .set (
      dsl.inputs ++= prototypes,
      dsl.outputs += result
    )


