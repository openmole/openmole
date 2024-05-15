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
import org.openmole.core.setter._
import org.openmole.core.workflow.dsl._

import scala.reflect.ClassTag

/**
 * Task to force the conversion of prototype to prototypes of arrays
 */
object ToArrayTask {

  /**
   * ToArrayTask from a set of prototypes (whatever the type T of each)
   *
   * @param prototypes
   * @return
   */
  def apply(prototypes: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    // FIXME seems to be never used ?
    ClosureTask("ToArrayTask"):
      (context, _, _) ⇒
        prototypes.map {
          p ⇒ Variable.unsecure(p.toArray, Array(context(p))(ClassTag(p.`type`.runtimeClass)))
        }
    .set (
      inputs ++= prototypes,
      outputs ++= prototypes.map(_.array)
    )

}

