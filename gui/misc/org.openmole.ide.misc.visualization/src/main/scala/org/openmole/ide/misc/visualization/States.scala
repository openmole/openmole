/*
 * Copyright (C) 2011 Mathieu Leclaire
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

package org.openmole.ide.misc.visualization

import org.openmole.core.model.execution.ExecutionState._

object States {
  def factory(s: States, state: ExecutionState, value: Int): States = {
    state match {
      case READY ⇒ new States(value, s.submitted, s.running)
      case SUBMITTED ⇒ new States(s.ready, value, s.running)
      case RUNNING ⇒ new States(s.ready, s.submitted, value)
      case _ ⇒ s
    }
  }
}

class States(val ready: Int, val submitted: Int, val running: Int)