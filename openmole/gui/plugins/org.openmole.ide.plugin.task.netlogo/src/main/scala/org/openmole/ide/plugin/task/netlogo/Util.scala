/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.plugin.task.netlogo

import java.io.File

object Util {
  implicit def stringToFile(s: String) = new File(s)
  implicit def fromWtoW(w: org.openmole.plugin.task.netlogo.NetLogoTask.Workspace): Workspace =
    new Workspace(w.location)

  case class Workspace(location: Either[(File, String), File]) {
    def this(workspace: File, script: String) = this(Left(workspace, script))
    def this(script: File) = this(Right(script))
    def coreObject = new org.openmole.plugin.task.netlogo.NetLogoTask.Workspace(location)
  }

  def toWorkspace(script: String, embedWS: Boolean) =
    if (embedWS) new Workspace(script.getParentFile, script)
    else new Workspace(script)

  def fromWorkspace(w: Workspace): (String, Boolean) = w.location match {
    case Left((w: File, s: String)) ⇒ (w.getAbsolutePath, true)
    case Right(s: File)             ⇒ (s.getAbsolutePath, false)

  }
}