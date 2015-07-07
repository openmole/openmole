/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.core.workflow.tools

import org.openmole.core.tools.service.Random
import org.openmole.core.workspace.Workspace
import org.openmole.tool.file.FilePackage

object CodeTool extends FilePackage {
  def namespace = s"${this.getClass.getPackage.getName}.CodeTool"

  def newRNG(seed: Long) = Random.newRNG(seed)
  def newFile(prefix: String = Workspace.fixedPrefix, suffix: String = Workspace.fixedPostfix) = Workspace.newFile(prefix, suffix)
  def newDir(prefix: String = Workspace.fixedDir) = Workspace.newDir(prefix)
}
