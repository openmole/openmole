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

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.dsl
import org.openmole.core.dsl._
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.task._

import scala.reflect.ClassTag

object FlattenTask:

  def apply[S](flatten: Val[Array[Array[S]]], in: Val[Array[S]])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("FlattenTask"): p =>
      import p.*
      implicit val sClassTag = ClassTag[S](Val.fromArray(in).`type`.runtimeClass)
      Variable(in, context(flatten).flatten.toArray[S])
    .set (
      dsl.inputs += flatten,
      dsl.outputs += in
    )


