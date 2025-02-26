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

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object AssignTask:

  def apply(assignments: (Val[?], Val[?])*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("AssignTask"): p =>
      assignments.map { case (from, to) => Variable.unsecure(to, p.context(from)) }
    .set (
      inputs ++= assignments.map(_._1),
      outputs ++= assignments.map(_._2)
    )

