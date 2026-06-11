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

import org.openmole.core.argument.Condition
import org.openmole.core.dsl._
import org.openmole.core.setter.DefinitionScope

object Switch:
  object Case:
    def apply(dsl: DSL): Case = Case(Condition.True, dsl)

  case class Case(condition: Condition, dsl: DSL)

  def apply(cases: Case*)(using scope: DefinitionScope = DefinitionScope.InternalScope("switch")) =
    val first = Strain(EmptyTask()) //set ((inputs, outputs) ++= c.dsl.inputs))
    val last = Strain(EmptyTask()) //set ((inputs, outputs) ++= c.dsl.outputs))

    cases.map {
      case Case(condition, dsl) =>
        (first -- dsl when condition) -- Slot(last)
    }.reduce[DSL](_ & _)


