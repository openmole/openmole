/**
 * Created by Romain Reuillon on 02/09/16.
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
package org.openmole.site.module

import org.openmole.core.pluginmanager.PluginManager
import org.openmole.plugin.task.netlogo5.NetLogo5Task
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.core.buildinfo._

object Module {

  def all = Seq[Module](
    Module("NetLogo5", "Explore NetLogo 5 simulation models", components[NetLogo5Task])
  )

  def components[T](implicit m: Manifest[T]) =
    PluginManager.pluginsForClass(m.erasure).toSeq

  def generate(modules: Seq[Module], baseDirectory: File, location: File ⇒ String) = {
    def allFiles = modules.flatMap(_.components)

    for {
      f ← allFiles.distinct
    } yield {
      val dest = baseDirectory / location(f)
      f copy dest
    }

    modules.map { m ⇒
      ModuleEntry(
        m.name,
        m.description,
        m.components.map(f ⇒ Component(location(f), f.hash.toString))
      )
    }
  }

}

case class Module(name: String, description: String, components: Seq[File])

