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

package org.openmole.plugin.environment.egi

import org.openmole.core.pluginmanager.PluginInfo
import org.openmole.core.preference.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = {
    PluginInfo.remove(this.getClass)
    ConfigurationInfo.remove(this.getClass)
  }

  override def start(context: BundleContext): Unit = {
    PluginInfo.add(this.getClass)
    ConfigurationInfo.add(
      this.getClass,
      ConfigurationInfo.list(EGIEnvironment) //++ ConfigurationInfo.list(DIRACEnvironment)
    )
  }
}
