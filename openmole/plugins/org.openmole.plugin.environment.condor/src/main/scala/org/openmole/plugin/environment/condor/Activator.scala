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

package org.openmole.plugin.environment.condor

import org.openmole.core.pluginmanager.{ KeyWord, PluginInfo }
import org.osgi.framework._

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = {
    PluginInfo.unregister(this)
  }

  override def start(context: BundleContext): Unit = {
    import org.openmole.core.pluginmanager.KeyWord._

    val keyWords: Vector[KeyWord] =
      Vector(
        Environment(classOf[CondorEnvironment[_]])
      )

    PluginInfo.register(this, namespaces = Vector(this.getClass.getPackage), keyWords = keyWords)
  }
}