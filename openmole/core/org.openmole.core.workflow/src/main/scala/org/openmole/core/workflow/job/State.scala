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

package org.openmole.core.workflow.job

object State extends Enumeration {

  /**
   * Enumeration of possible job states
   *
   * @param name    name of the state
   * @param isFinal Get if the state is a final state. Meaning there is for the [[Job]] to change state again.
   */
  case class State(name: String, isFinal: Boolean = false) extends Val(name)

  /**
   *
   * The job as been created and is ready to be executed.
   *
   */
  val READY = new State("Ready")

  /**
   *
   * The job is being executed.
   *
   */
  val RUNNING = new State("Running")

  /**
   *
   * The job has successfully ended.
   *
   */
  val COMPLETED = new State("Completed", true)

  /**
   *
   * The job has failed, an uncaught exception has been raised
   * to the workflow engine.
   *
   */
  val FAILED = new State("Failed", true)

}
