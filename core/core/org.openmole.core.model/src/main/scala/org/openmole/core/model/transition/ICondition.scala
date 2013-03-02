/*
 * Copyright (C) 2010 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.transition

import org.openmole.core.model.data.Context

object ICondition {

  val True = new ICondition {
    def evaluate(context: Context): Boolean = true
  }

  val False = new ICondition {
    def evaluate(context: Context): Boolean = false
  }

  implicit def function2IConditionConverter(f: Context ⇒ Boolean) = new ICondition {
    override def evaluate(context: Context) = f(context)
  }

}

trait ICondition {

  /**
   *
   * Evaluate the value of this condition in a given context.
   *
   * @param context the context in which the condition is evaluated
   * @return the value of this condition
   */
  def evaluate(context: Context): Boolean

}
