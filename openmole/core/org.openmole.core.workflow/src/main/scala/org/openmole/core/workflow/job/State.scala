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

object State:

  /**
   *
   * The job as been created and is ready to be executed.
   *
   */
  final val READY: State = 0.toByte

  /**
   *
   * The job is being executed.
   *
   */
  final val RUNNING: State = 1.toByte

  /**
   *
   * The job has successfully ended.
   *
   */
  final val COMPLETED: State = 2.toByte

  /**
   *
   * The job has failed, an uncaught exception has been raised
   * to the workflow engine.
   *
   */
  final val FAILED: State = 3.toByte

