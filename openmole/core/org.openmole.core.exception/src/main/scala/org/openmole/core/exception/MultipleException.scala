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

object MultipleException {
  def apply(exceptions: Iterable[Throwable]): Throwable = {
    if (exceptions.size == 1) exceptions.head
    else new MultipleException(exceptions)
  }
}

/**
 * An Exception wrapping several exceptions
 * @param exceptions
 */
class MultipleException(exceptions: Iterable[Throwable]) extends Exception with Iterable[Throwable] {

  def iterator: Iterator[Throwable] = exceptions.iterator

  override def toString() =
    this.getClass.getName + ":\n" +
      exceptions.map(e => ExceptionUtils.prettify(e)).mkString("----------------------------------------\n")

}
