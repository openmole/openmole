package org.openmole.gui.plugin.environment.egi

import java.io.File

import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.tool.server.Utils._
import org.openmole.plugin.environment.egi.P12Certificate

/*
 * Copyright (C) 13/01/17 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object Utils {

  def authenticationFile(keyFileName: String): File = new File(authenticationKeysFile, keyFileName)
}
