/**
 * Created by Romain Reuillon on 29/11/16.
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
package org.openmole.gui.ext.tool.server

import org.openmole.gui.ext.data.{ AuthenticationPluginFactory, GUIPluginAsJS, MethodAnalysisPlugin, WizardPluginFactory }
import org.openmole.core.services._

import scala.collection.JavaConverters._

object GUIPlugin {
  private lazy val plugins = new java.util.concurrent.ConcurrentHashMap[AnyRef, GUIPlugin]().asScala

  def toGUIPlugins(c: Class[_]): GUIPluginAsJS = GUIPluginAsJS(c.getName)

  def routers = plugins.flatMap(_._2.router).toSeq

  def authentications: Seq[GUIPluginAsJS] = plugins.values.flatMap(_.authentication).map(toGUIPlugins).toSeq
  def wizards: Seq[GUIPluginAsJS] = plugins.values.flatMap(_.wizard).map(toGUIPlugins).toSeq

  def analysis: Seq[(String, GUIPluginAsJS)] = plugins.values.flatMap(_.analysis).map(a ⇒ a._1 -> toGUIPlugins(a._2)).toSeq

  def unregister(key: AnyRef) = GUIPlugin.plugins -= key
  def register(key: AnyRef, info: GUIPlugin) = GUIPlugin.plugins += key → info
}

case class GUIPlugin(
  router:         Option[Services ⇒ OMRouter]                        = None,
  authentication: Option[Class[_ <: AuthenticationPluginFactory]]    = None,
  wizard:         Option[Class[_ <: WizardPluginFactory]]            = None,
  analysis:       Option[(String, Class[_ <: MethodAnalysisPlugin])] = None
)
