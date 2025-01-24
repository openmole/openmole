/*
 * Copyright (C) 2015 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.sge

import org.openmole.core.highlight.HighLight
import org.openmole.core.pluginregistry.{ PluginInfo, PluginRegistry }
import org.osgi.framework._

class Activator extends BundleActivator:
  override def stop(context: BundleContext): Unit =
    PluginRegistry.unregister(this)

  override def start(context: BundleContext): Unit =
    import org.openmole.core.highlight.HighLight._

    val keyWords: Vector[HighLight] =
      Vector(
        EnvironmentHighLight(classOf[SGEEnvironment[?]])
      )

    PluginRegistry.register(this, nameSpaces = Vector(this.getClass.getPackage), highLight = keyWords)
