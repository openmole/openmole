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
import org.openmole.plugin.task.netlogo.NetLogoTask.Workspace

object Util {
  implicit def stringToFile(s: String) = new File(s)

  def toWorkspace(script: String, embedWS: Boolean) =
    if (embedWS) new Workspace(script.getParentFile, script)
    else new Workspace(script)

  def fromWorkspace(w: Workspace): (String, Boolean) = w.location match {
    case Left((w: File, s: String)) ⇒ (w.getAbsolutePath, true)
    case Right(s: File)             ⇒ (s.getAbsolutePath, false)

  }
}