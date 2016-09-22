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

import fr.iscpif.gridscale.storage._
import fr.iscpif.gridscale.http.HTTPStorage
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workspace.{ ConfigurationLocation, Workspace }
import org.openmole.tool.file._
import org.openmole.tool.stream._
import org.openmole.core.buildinfo

package object module {

  val moduleIndexes = ConfigurationLocation("Module", "Indexes", Some(Seq[String](buildinfo.moduleAddress)))

  Workspace setDefault moduleIndexes

  lazy val pluginDirectory = Workspace.location / "plugins"
  pluginDirectory.mkdirs

  lazy val moduleDirectory =
    if (buildinfo.development) Workspace.newDir("modules")
    else Workspace.location / "modules" / buildinfo.version

  moduleDirectory.mkdirs

  def allModules =
    (pluginDirectory.listFilesSafe ++ moduleDirectory.listFilesSafe).flatMap(PluginManager.listBundles)

  import org.json4s._
  import org.json4s.jackson.Serialization
  implicit val formats = Serialization.formats(NoTypeHints)

  def modules(url: String) = HTTPStorage.download(url)(Serialization.read[Seq[Module]](_))
  def selectableModules(url: String) = modules(url).map(m ⇒ SelectableModule(Storage.parent(url).get, m))

  case class SelectableModule(baseURL: String, module: Module)

  def install(modules: Seq[SelectableModule]) = Workspace.withTmpDir { dir ⇒
    case class DownloadableComponent(baseURL: String, component: Component)
    val downloadableComponents = modules.flatMap { m ⇒ m.module.components.map(c ⇒ DownloadableComponent(m.baseURL, c)) }
    val hashes = downloadableComponents.map(_.component.hash).distinct.toSet -- PluginManager.bundleHashes.map(_.toString)
    val files = downloadableComponents.filter(c ⇒ hashes.contains(c.component.hash)).map {
      c ⇒
        val f = dir / Storage.name(c.component.location)
        HTTPStorage.download(Storage.child(c.baseURL, c.component.location))(_.copy(f))
        f
    }
    addPluginsFiles(files, true)
  }

  def components[T](implicit m: Manifest[T]) = PluginManager.pluginsForClass(m.erasure).toSeq

  def addPluginsFiles(files: Seq[File], move: Boolean, directory: File = pluginDirectory): Seq[(File, Throwable)] = synchronized {
    val destinations = files.map { file ⇒ file → (directory / file.getName) }

    destinations.filter(_._2.exists).toList match {
      case Nil ⇒
        val plugins =
          destinations.map {
            case (file, dest) ⇒
              if (!move) file copy dest else file move dest
              dest
          }
        PluginManager.tryLoad(plugins).toSeq
      case l ⇒
        l.map(l ⇒ l._1 → new FileAlreadyExistsException(s"Plugin with file name ${l._1.getName} is already present in the plugin directory"))
    }
  }
}
