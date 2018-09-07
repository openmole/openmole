package org.openmole.gui.ext.tool.server

import org.openmole.core.module
import org.openmole.core.pluginmanager._
import org.openmole.gui.ext.data._
import org.openmole.core.workspace.Workspace

import scala.io.Source

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

import org.openmole.tool.file._

object Utils {

  def authenticationKeysFile(implicit workspace: Workspace) = workspace.persistentDir / "keys"

  def webUIDirectory()(implicit workspace: Workspace) = workspace.location /> "webui"

  implicit def safePathToFile(s: SafePath)(implicit context: org.openmole.gui.ext.data.ServerFileSystemContext, workspace: Workspace): File = {
    context match {
      case _: org.openmole.gui.ext.data.ProjectFileSystem ⇒ getFile(webUIDirectory, s.path)
      case _ ⇒ getFile(new File(""), s.path)
    }

  }

  def getFile(root: File, paths: Seq[String]): File = {
    def getFile0(paths: Seq[String], accFile: File): File = {
      if (paths.isEmpty) accFile
      else getFile0(paths.tail, new File(accFile, paths.head))
    }

    getFile0(paths, root)
  }

  def lines(safePath: SafePath)(implicit context: org.openmole.gui.ext.data.ServerFileSystemContext, workspace: Workspace) = {
    Source.fromFile(safePathToFile(safePath)).getLines.toArray
  }

  implicit class ASafePath(safePath: SafePath) {
    def write(content: String)(implicit context: org.openmole.gui.ext.data.ServerFileSystemContext, workspace: Workspace) = {
      val f: File = safePathToFile(safePath)
      f.content = content
    }
  }

  def addPlugins(safePaths: Seq[SafePath])(implicit workspace: Workspace): Seq[Error] = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val files: Seq[File] = safePaths.map {
      safePathToFile
    }
    addFilePlugins(files)
  }

  def addFilePlugins(files: Seq[File])(implicit workspace: Workspace): Seq[Error] = {
    val errors = org.openmole.core.module.addPluginsFiles(files, false, Some(org.openmole.core.module.pluginDirectory))
    errors.map(e ⇒ ErrorBuilder(e._2))
  }

  def removePlugin(plugin: Plugin)(implicit workspace: Workspace): Unit = synchronized {
    val file = module.pluginDirectory / plugin.name
    val allDependingFiles = PluginManager.allDepending(file, b ⇒ !b.isProvided)
    val bundle = PluginManager.bundle(file)
    bundle.foreach(PluginManager.remove)
    allDependingFiles.filter(f ⇒ !PluginManager.bundle(f).isDefined).foreach(_.recursiveDelete)
    file.recursiveDelete
  }

}
