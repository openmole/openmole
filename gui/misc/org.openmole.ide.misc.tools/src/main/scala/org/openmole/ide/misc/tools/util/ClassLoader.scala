/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.misc.tools.util

import java.io.File
import groovy.lang.GroovyShell
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.exception.UserBadDataError

object ClassLoader {

  def loadExtraPlugins = {
    Workspace.pluginDirLocation.list.map {
      f ⇒ new File(Workspace.pluginDirLocation + "/" + f)
    } foreach {
      PluginManager.load
    }
  }
}