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
import org.openmole.core.preference.ConfigurationLocationRegistry
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {
  override def stop(context: BundleContext): Unit = {
    PluginInfo.unregister(this)
  }

  override def start(context: BundleContext): Unit = {
    import org.openmole.core.pluginmanager.KeyWord._

    val keyWords = Vector(
      WordKeyWord("byte"),
      WordKeyWord("bytes"),
      WordKeyWord("kilobyte"),
      WordKeyWord("kilobytes"),
      WordKeyWord("megabyte"),
      WordKeyWord("megabytes"),
      WordKeyWord("gigabyte"),
      WordKeyWord("gigabytes"),
      WordKeyWord("terabyte"),
      WordKeyWord("terabytes"),
      WordKeyWord("petabyte"),
      WordKeyWord("petabytes"),
      WordKeyWord("exabyte"),
      WordKeyWord("exabytes"),
      WordKeyWord("zettabyte"),
      WordKeyWord("zettabytes"),
      WordKeyWord("yottabyte"),
      WordKeyWord("yottabytes"),
      WordKeyWord("nanosecond"),
      WordKeyWord("nanoseconds"),
      WordKeyWord("microsecond"),
      WordKeyWord("microseconds"),
      WordKeyWord("millisecond"),
      WordKeyWord("milliseconds"),
      WordKeyWord("second"),
      WordKeyWord("seconds"),
      WordKeyWord("minute"),
      WordKeyWord("minutes"),
      WordKeyWord("hour"),
      WordKeyWord("hours"),
      WordKeyWord("day"),
      WordKeyWord("days")
    )

    PluginInfo.register(this, keyWords = keyWords)
  }
}
