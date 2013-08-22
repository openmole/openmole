/*
 * Copyright (C) 22/02/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.mole

import java.io.{ PrintStream, File }
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.workspace.AuthenticationProvider

object ExecutionContext {
  def apply(out: PrintStream, directory: Option[File]) = {
    val (_out, _directory) = (out, directory)
    new ExecutionContext {
      def out = _out
      def directory = _directory
    }
  }

  lazy val local = ExecutionContext(System.out, None)
}

trait ExecutionContext {
  def out: PrintStream
  def directory: Option[File]
  def relativise(f: String): File =
    directory.map(_.child(f)).getOrElse(new File(f))

  def copy(
    out: PrintStream = out,
    directory: Option[File] = directory) = ExecutionContext(out, directory)

}
