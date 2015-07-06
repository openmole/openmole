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
import org.openmole.gui.ext.data.SafePath._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.FileExtension._
import java.io.File
import java.net.URI

object Utils {

  implicit def fileToExtension(f: File): FileExtension = f.getName.split('.').last match {
    case "oms"                   ⇒ OMS
    case "scala"                 ⇒ SCALA
    case "sh"                    ⇒ SH
    case "nlogo" | "csv" | "txt" ⇒ NO_EXTENSION
    case _                       ⇒ BINARY
  }

  implicit def fileToTreeNodeData(f: File): TreeNodeData = TreeNodeData(f.getName, SafePath(f.toURI.toString, f.getName, f), f.isDirectory, f.length, readableByteCount(f.length))

  implicit def seqfileToSeqTreeNodeData(fs: Seq[File]): Seq[TreeNodeData] = fs.map {
    fileToTreeNodeData(_)
  }

  implicit def fileToSafePath(f: File): SafePath = SafePath(f.toURI.toString, f.getName, f)

  implicit def fileToOptionSafePath(f: File): Option[SafePath] = Some(fileToSafePath(f))

  implicit def safePathToFile(s: SafePath): File = new File(s.path)

  def getParent(f: File) = f.getParentFile.getName

  val workspaceProjectFile = Workspace.file("webui/projects")

  val workspaceRoot = workspaceProjectFile.getParentFile

  val authenticationKeysFile = Workspace.file("persistent/keys")

  def listFiles(path: SafePath): Seq[TreeNodeData] = new File(path.path).listFilesSafe.toSeq

}
