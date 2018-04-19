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

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._

import scala.reflect.ClassTag

object ToArrayTask {

  def apply(prototypes: Val[T] forSome { type T }*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    ClosureTask("ToArrayTask") {
      (context, _, _) ⇒
        prototypes.map {
          p ⇒ Variable.unsecure(p.toArray, Array(context(p))(ClassTag(p.`type`.runtimeClass)))
        }
    } set (
      inputs += (prototypes: _*),
      outputs += (prototypes.map(_.array): _*)
    )

}

