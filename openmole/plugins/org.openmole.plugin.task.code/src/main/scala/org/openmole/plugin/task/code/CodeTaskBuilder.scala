/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.task.code

import java.io.File
import org.openmole.plugin.task.external.ExternalTaskBuilder
import scala.collection.mutable.ListBuffer

abstract class CodeTaskBuilder extends ExternalTaskBuilder {
  private var _imports = new ListBuffer[String]
  private var _libraries = new ListBuffer[File]

  def imports = _imports.toList
  def libraries = _libraries.toList

  def addImport(s: String) = {
    _imports += s
    this
  }

  def addLib(l: File) = {
    _libraries += l
    this
  }

}