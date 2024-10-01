/**
 * Created by Romain Reuillon on 19/09/16.
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
 *
 */
package org.openmole.core

import java.io.File
import java.nio.file.FileAlreadyExistsException

import gridscale.http
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.tool.file._
import org.openmole.tool.stream._
import org.openmole.core.argument._
import org.openmole.core.context._
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.tool.random.RandomProvider

package object module:

  def indexes(implicit preference: Preference, randomProvider: RandomProvider, newFile: TmpDirectory, fileService: FileService) =
    preference(ModuleIndex.moduleIndexes).map(ExpandedString(_).from(Context("version" → buildinfo.version)))

  def pluginDirectory(implicit workspace: Workspace) = workspace.location /> "plugins"
  def moduleDirectory(implicit workspace: Workspace) = workspace.persistentDir /> "modules" /> buildinfo.version.major

  def allModules(implicit workspace: Workspace) =
    (pluginDirectory.listFilesSafe ++ moduleDirectory.listFilesSafe).flatMap(PluginManager.listBundles)

  import org.json4s._
  import org.json4s.jackson.Serialization
  implicit val formats: Formats = Serialization.formats(NoTypeHints)

  def modules(url: String) = Serialization.read[Seq[Module]](http.get(url))
  def selectableModules(url: String) = modules(url).map(m ⇒ SelectableModule(gridscale.RemotePath.parent(url).get, m))

  case class SelectableModule(baseURL: String, module: Module)

  def install(modules: Seq[SelectableModule])(implicit newFile: TmpDirectory, workspace: Workspace) =
    TmpDirectory.withTmpDir: dir ⇒
      case class DownloadableComponent(baseURL: String, component: Component)
      val downloadableComponents = modules.flatMap { m ⇒ m.module.components.map(c ⇒ DownloadableComponent(m.baseURL, c)) }
      val hashes = downloadableComponents.map(_.component.hash).distinct.toSet -- PluginManager.bundleHashes.map(_.toString)
      val files =
        downloadableComponents.filter(c ⇒ hashes.contains(c.component.hash)).map: c ⇒
          val f = dir / gridscale.RemotePath.name(c.component.name)
          http.getStream(gridscale.RemotePath.child(c.baseURL, c.component.name))(_.copy(f))
          f

      addPluginsFiles(files, true, moduleDirectory)

  def components[T](implicit m: Manifest[T]) = PluginManager.pluginsForClass(m.erasure).toSeq
  def components(o: Object) = PluginManager.pluginsForClass(o.getClass).toSeq

  def addPluginsFiles(files: Seq[File], move: Boolean, directory: File)(implicit workspace: Workspace, newFile: TmpDirectory): Seq[(File, Throwable)] =
    PluginManager.tryLoad(files).toSeq

