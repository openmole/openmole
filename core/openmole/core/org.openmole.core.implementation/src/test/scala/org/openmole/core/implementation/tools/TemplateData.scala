/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.implementation.tools

import java.io.File
import java.io.PrintWriter

object TemplateData {

  def templateFile(): File = {
    val template = File.createTempFile("file", ".test")
    val writer = new PrintWriter(template)
    writer.println("My first line")
    writer.println("${2*3}")
    writer.print("${\"I am ${6*5} year old\"}")
    writer.close
    template
  }

  def targetFile(): File = {
    val target = File.createTempFile("target", ".test")
    val writert = new PrintWriter(target)
    writert.println("My first line")
    writert.println("6")
    writert.print("I am 30 year old")
    writert.close
    target
  }
}
