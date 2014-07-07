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

import org.openmole.misc.exception.UserBadDataError

object Workspace {

  implicit def stringToFile(s: String) = new File(s)
  implicit def fromWtoW(w: org.openmole.plugin.task.netlogo.NetLogoTask.Workspace): Workspace = FilledWorkspace(w.location)

  def toWorkspace(script: String, embedWS: Boolean) =
    if (script.isEmpty) EmptyWorkspace
    else if (embedWS) FilledWorkspace(Left((script.getParentFile, script.getName)))
    else FilledWorkspace(Right(script))

  def fromWorkspace(w: Workspace): (File, Boolean) =
    w match {
      case EmptyWorkspace                ⇒ ("", false)
      case FilledWorkspace(Right(s))     ⇒ (s, false)
      case FilledWorkspace(Left((w, s))) ⇒ (new File(w, s), true)
    }

  def toCoreObject(w: Workspace) =
    w match {
      case w: FilledWorkspace ⇒ new org.openmole.plugin.task.netlogo.NetLogoTask.Workspace(w.location)
      case EmptyWorkspace     ⇒ throw new UserBadDataError("Script location has not been set yet")
    }

}

sealed trait Workspace

case object EmptyWorkspace extends Workspace

case class FilledWorkspace(location: Either[(File, String), File]) extends Workspace

