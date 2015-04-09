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
package org.openmole.rest.messages

object Error {
  def apply(e: Throwable): Error =
    Error(e.getMessage, Some(e.getStackTrace.map(e ⇒ s"\tat$e").reduceLeft((prev, next) ⇒ s"$prev\n$next")))
}
case class Error(message: String, stackTrace: Option[String])
case class Token(token: String, duration: Long)
