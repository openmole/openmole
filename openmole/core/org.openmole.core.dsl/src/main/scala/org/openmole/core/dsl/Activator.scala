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

import org.openmole.core.pluginregistry.{ PluginInfo, PluginRegistry }
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator:
  override def stop(context: BundleContext): Unit = PluginRegistry.unregister(this)

  override def start(context: BundleContext): Unit =
    import org.openmole.core.highlight.HighLight._

    val highLight = Vector(
      WordHighLight("byte"),
      WordHighLight("bytes"),
      WordHighLight("kilobyte"),
      WordHighLight("kilobytes"),
      WordHighLight("megabyte"),
      WordHighLight("megabytes"),
      WordHighLight("gigabyte"),
      WordHighLight("gigabytes"),
      WordHighLight("terabyte"),
      WordHighLight("terabytes"),
      WordHighLight("petabyte"),
      WordHighLight("petabytes"),
      WordHighLight("exabyte"),
      WordHighLight("exabytes"),
      WordHighLight("zettabyte"),
      WordHighLight("zettabytes"),
      WordHighLight("yottabyte"),
      WordHighLight("yottabytes"),
      WordHighLight("nanosecond"),
      WordHighLight("nanoseconds"),
      WordHighLight("microsecond"),
      WordHighLight("microseconds"),
      WordHighLight("millisecond"),
      WordHighLight("milliseconds"),
      WordHighLight("second"),
      WordHighLight("seconds"),
      WordHighLight("minute"),
      WordHighLight("minutes"),
      WordHighLight("hour"),
      WordHighLight("hours"),
      WordHighLight("day"),
      WordHighLight("days"),
      WordHighLight("TrueFalse")
    )

    PluginRegistry.register(this, highLight = highLight)

