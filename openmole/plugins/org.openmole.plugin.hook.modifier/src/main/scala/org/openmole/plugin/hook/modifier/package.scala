/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.plugin.hook

import org.openmole.core.workflow.mole._
import org.openmole.core.expansion._
import org.openmole.core.workflow.builder.DefinitionScope

package object modifier {

  implicit class HookModifierDecorator(h: Hook) {
    def when(condition: Condition)(implicit definitionScope: DefinitionScope) = ConditionHook(h, condition)
    def when(condition: String)(implicit definitionScope: DefinitionScope) = ConditionHook(h, condition)
    def condition(condition: Condition)(implicit definitionScope: DefinitionScope) = when(condition)
    def condition(condition: String)(implicit definitionScope: DefinitionScope) = when(condition)
  }

}
