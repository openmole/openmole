/**
 * Copyright (C) 2015 Jonathan Passerat-Palmbach
 * Copyright (C) 2015 Mathieu Leclaire
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

package org.openmole.plugin.task

import org.openmole.plugin.task.systemexec.SystemExecPackage
import org.openmole.core.dsl._

package care {
  trait CAREPackage extends SystemExecPackage {
    lazy val hostFiles = new {
      def +=[T: CARETaskBuilder](hostFile: String, binding: Option[String] = None) =
        implicitly[CARETaskBuilder[T]].hostFiles add (hostFile, binding)
    }
  }
}

package object care extends CAREPackage {
  val workDirectoryParse = """-w\s*['"](.*)['"]\s*\\""".r

  def workDirectoryLine(content: Seq[String]) =
    content.filter(_.trim.startsWith("-w")).lastOption.flatMap(parseWorkDirectory)

  def parseWorkDirectory(line: String) =
    line.trim match {
      case workDirectoryParse(wd) ⇒ Some(wd)
      case _                      ⇒ None
    }
}
