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
package org.openmole.rest

import java.io.{ PrintWriter, StringWriter }

package object message {

  object Error {
    def apply(e: Throwable): Error = {
      val sw = new StringWriter()
      e.printStackTrace(new PrintWriter(sw))
      Error(e.getMessage, Some(sw.toString))
    }
  }
  case class Error(message: String, stackTrace: Option[String] = None)
  case class Token(token: String, duration: Long)
  case class ExecutionId(id: String)
  case class Output(output: String)

  type ExecutionState = String
  val running: ExecutionState = "running"
  val finished: ExecutionState = "finished"
  val canceled: ExecutionState = "canceled"

  case class State(
    state: ExecutionState,
    error: Option[Error])
}