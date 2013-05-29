/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.model.execution

object ExecutionState extends Enumeration {

  class ExecutionState(val name: String, val isFinal: Boolean = false) extends Val(name)

  val READY = new ExecutionState("Ready")
  val SUBMITTED = new ExecutionState("Submitted")
  val RUNNING = new ExecutionState("Running")
  val DONE = new ExecutionState("Done")
  val FAILED = new ExecutionState("Failed", true)
  val KILLED = new ExecutionState("Killed", true)
}

