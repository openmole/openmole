/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.plugin.task.scala

import org.openmole.core.model.task.PluginSet
import org.openmole.plugin.task.code.CodeTaskBuilder

import scala.collection.mutable.ListBuffer

class ScalaTaskBuilder(code: String)(implicit plugins: PluginSet) extends CodeTaskBuilder { builder ⇒

  val usedClasses = ListBuffer[Class[_]]()

  def addClassUse(c: Class[_]) = usedClasses += c

  addImport("org.openmole.misc.tools.service.Random.newRNG")
  addImport("org.openmole.misc.workspace.Workspace.newFile")
  addImport("org.openmole.misc.workspace.Workspace.newDir")

  def toTask =
    new ScalaTask(code, usedClasses) with builder.Built
}
