/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.ui.console.internal.command.initializer


import org.openmole.misc.workspace.Workspace

class WorkspaceInitializer extends IInitializer {

  override def initialize(obj: Object, c: Class[_]) = {
    val workspace = obj.asInstanceOf[Workspace.type]
    val message = (if(workspace.passwordChoosen) "Enter your OpenMOLE password" else "OpenMOLE Password has not been set yet, choose a  password") + "  (for preferences encryption):"
    
    do {workspace.password_=(new jline.ConsoleReader().readLine(message, '*'))} while(!workspace.passwordIsCorrect)
  }
}
