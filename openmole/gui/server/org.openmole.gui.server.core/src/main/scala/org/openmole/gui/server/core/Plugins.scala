/**
 * Created by Romain Reuillon on 30/11/16.
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
package org.openmole.gui.server.core

import org.openmole.core.pluginmanager._
import org.openmole.tool.file._
import org.openmole.tool.stream._
import collection.JavaConverters._

object Plugins {

  def gatherJSIRFiles(dest: File) = {
    def bundles =
      PluginManager.bundles.filter { b ⇒
        !b.openMOLEScope.exists(_.toLowerCase == "gui-provided")
      }

    for {
      b ← bundles
      jsir ← Option(b.findEntries("/", "*.sjsir", true)).map(_.asScala).getOrElse(Seq.empty)
    } {
      val destFile = dest / jsir.getPath
      destFile.getParentFile.mkdirs()
      b.classLoader.getResourceAsStream(jsir.getPath) copy destFile
    }
  }

}
