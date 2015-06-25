package org.openmole.gui.server.core

/*
 * Copyright (C) 16/04/15 // mathieu.leclaire@openmole.org
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

import org.openmole.tool.file._
import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data._
import java.io.File
import java.net.URI

object Utils {

  implicit def fileToTreeNodeData(f: File): TreeNodeData = TreeNodeData(f.getName, f.getCanonicalPath, f.isDirectory, f.length, readableByteCount(f.length))

  implicit def seqfileToSeqTreeNodeData(fs: Seq[File]): Seq[TreeNodeData] = fs.map {
    fileToTreeNodeData(_)
  }

  implicit def uriToFile(uri: URI): File = new File(uri.getPath)

  val workspaceProjectFile = Workspace.file("webui/projects")

  def listFiles(path: String): Seq[TreeNodeData] = new File(path).listFiles.toSeq

}
