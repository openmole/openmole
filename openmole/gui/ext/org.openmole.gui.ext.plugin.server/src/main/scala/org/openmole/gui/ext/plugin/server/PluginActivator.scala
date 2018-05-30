package org.openmole.gui.ext.plugin.server

/**
 * Created by Romain Reuillon on 28/11/16.
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

import org.openmole.gui.ext.data.{ AuthenticationPluginFactory, GUIPluginAsJS, VersioningPluginFactory, WizardPluginFactory }
import org.osgi.framework._

import collection.JavaConverters._

object PluginActivator {
  lazy val plugins = new java.util.concurrent.ConcurrentHashMap[Class[_], PluginInfo]().asScala

  implicit def classesToGUIPlugins(cs: Seq[Class[_]]): Seq[GUIPluginAsJS] = cs.map { c ⇒
    GUIPluginAsJS(c.getName)
  }

  private def instances = plugins.values.toSeq.map {
    _.clientInstance
  }

  def authentications: Seq[GUIPluginAsJS] =
    instances.filter {
      classOf[AuthenticationPluginFactory].isAssignableFrom
    }

  def wizards: Seq[GUIPluginAsJS] =
    instances.filter {
      classOf[WizardPluginFactory].isAssignableFrom
    }

  def versioning: Seq[GUIPluginAsJS] =
    instances.filter {
      classOf[VersioningPluginFactory].isAssignableFrom
    }
}

trait PluginActivator extends BundleActivator {
  def info: PluginInfo

  override def stop(context: BundleContext): Unit =
    PluginActivator.plugins -= info.clientInstance

  override def start(context: BundleContext): Unit = {
    PluginActivator.plugins += info.clientInstance → info
  }
}
