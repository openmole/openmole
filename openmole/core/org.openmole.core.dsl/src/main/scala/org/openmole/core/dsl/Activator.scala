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

package org.openmole.core.dsl

import org.openmole.core.pluginmanager.PluginInfo
import org.openmole.core.preference.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = {
    PluginInfo.unregister(this)
  }

  override def start(context: BundleContext): Unit = {
    import org.openmole.core.pluginmanager.KeyWord._

    val keyWords = Vector(
      Word("byte"),
      Word("bytes"),
      Word("kilobyte"),
      Word("kilobytes"),
      Word("megabyte"),
      Word("megabytes"),
      Word("gigabyte"),
      Word("gigabytes"),
      Word("terabyte"),
      Word("terabytes"),
      Word("petabyte"),
      Word("petabytes"),
      Word("exabyte"),
      Word("exabytes"),
      Word("zettabyte"),
      Word("zettabytes"),
      Word("yottabyte"),
      Word("yottabytes"),
      Word("nanosecond"),
      Word("nanoseconds"),
      Word("microsecond"),
      Word("microseconds"),
      Word("millisecond"),
      Word("milliseconds"),
      Word("second"),
      Word("seconds"),
      Word("minute"),
      Word("minutes"),
      Word("hour"),
      Word("hours"),
      Word("day"),
      Word("days")
    )

    PluginInfo.register(this, keyWords = keyWords)
  }
}
