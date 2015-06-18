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
package org.openmole.plugin.tool.file

import org.openmole.core.workflow.data._
import java.io.File

import org.openmole.core.workflow.tools.ExpandedString

object FileProvider {
  implicit def fileToFileProvider(file: File) = SimpleFileProvider(file)
  def apply(directory: File, name: ExpandedString) = ExpandedProvider(directory, name)
}

trait FileProvider {
  def apply(context: ⇒ Context)(implicit rng: RandomProvider): File
}

case class SimpleFileProvider(file: File) extends FileProvider {
  def apply(context: ⇒ Context)(implicit rng: RandomProvider) = file
}

case class ExpandedProvider(directory: File, name: ExpandedString) extends FileProvider {
  def apply(context: ⇒ Context)(implicit rng: RandomProvider) = new File(directory, name.from(context))
}
