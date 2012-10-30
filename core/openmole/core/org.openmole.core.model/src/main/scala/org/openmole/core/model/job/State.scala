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

package org.openmole.core.model.job

object State extends Enumeration {

  /**
   * @param isFinal Get if the state is a final state. Meaning there is no way
   * it the {@link IMoleJob} state can change again.
   */
  case class State(name: String, val isFinal: Boolean = false) extends Val(name)

  /**
   *
   * The job as been created and is ready to be excuted.
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
   * The job has sucessfully ended.
   *
   */
  val COMPLETED = new State("Completed", true)

  /**
   *
   * The job has failed, an uncatched exception has been raised
   * to the workflow engine.
   *
   */
  val FAILED = new State("Failed", true)

  /**
   *
   * The job has been canceled.
   *
   */
  val CANCELED = new State("Canceled", true)
}
