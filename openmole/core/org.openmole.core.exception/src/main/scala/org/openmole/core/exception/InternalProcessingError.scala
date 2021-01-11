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

package org.openmole.core.exception

object InternalProcessingError {

  def apply(message: String, exception: Throwable = null) =
    new InternalProcessingError(message, exception)

}

/**
 * An exception occurred during the processing of a task
 * @param message
 * @param exception
 */
class InternalProcessingError(message: String, exception: Throwable = null) extends Exception(message, exception) {
  def this(exception: Throwable, message: String) = this(message, exception)
}
