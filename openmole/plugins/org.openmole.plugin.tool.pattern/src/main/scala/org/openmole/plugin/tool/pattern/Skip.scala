/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.plugin.tool.pattern

import org.openmole.core.dsl._
import org.openmole.core.argument._
import org.openmole.core.setter.DefinitionScope

object Skip {

  def apply(dsl: DSL, condition: Condition)(implicit definitionScope: DefinitionScope = DefinitionScope.InternalScope("skip")) = {
    val first = Strain(EmptyTask())
    val last = Strain(EmptyTask())

    ((first -- dsl when !condition) -- last) &
      (first -- Slot(last) when condition)
  }

}

object If {

  def apply(dsl: DSL, condition: Condition)(implicit definitionScope: DefinitionScope = DefinitionScope.InternalScope("if")) =
    Skip(dsl = dsl, condition = !condition)

}