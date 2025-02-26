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
package org.openmole.gui.server.ext

import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*
import org.openmole.core.services.*

import scala.collection.JavaConverters.*

object GUIPluginRegistry:
  private lazy val plugins = new java.util.concurrent.ConcurrentHashMap[AnyRef, GUIPluginInfo]().asScala

  def toGUIPlugins(c: Class[?]): GUIPluginAsJS = c.getName

  def routers: Seq[Services => OMRouter] = plugins.flatMap(_._2.router).toSeq

  def authentications: Seq[String] = plugins.values.flatMap(_.authentication).map(toGUIPlugins).toSeq
  def wizards: Seq[String] = plugins.values.flatMap(_.wizard).map(toGUIPlugins).toSeq

  def analysis: Seq[(String, String)] = plugins.values.flatMap(_.analysis).map(a => a._1 -> toGUIPlugins(a._2)).toSeq

  def all = plugins.values

  def unregister(key: AnyRef) = GUIPluginRegistry.plugins -= key
  def register(key: AnyRef, info: GUIPluginInfo) = GUIPluginRegistry.plugins += key → info

case class GUIPluginInfo(
  router:         Option[Services => OMRouter]    = None,
  authentication: Option[Class[?]]               = None,
  wizard:         Option[Class[?]]               = None,
  analysis:       Option[(String, Class[?])]     = None)
