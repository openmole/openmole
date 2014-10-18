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

import org.openmole.core.implementation.mole.HookBuilder
import org.openmole.core.implementation.transition.Condition
import org.openmole.core.model.mole.IHook
import org.openmole.core.model.transition.ICondition

package object modifier {

  implicit class HookModifierDecorator(h: IHook) {
    def when(condition: ICondition) = ConditionHook(h, condition)
    def when(condition: String) = ConditionHook(h, Condition(condition))
    def condition(condition: ICondition) = when(condition)
    def condition(condition: String) = when(condition)
  }

  implicit class HookBuilderModifierDecorator(h: HookBuilder) {
    def when(condition: ICondition) = h.toHook.condition(condition)
    def when(condition: String) = h.toHook.condition(condition)
    def condition(condition: ICondition) = when(condition)
    def condition(condition: String) = when(condition)
  }
}
