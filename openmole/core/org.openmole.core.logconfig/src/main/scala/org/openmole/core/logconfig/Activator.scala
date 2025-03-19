/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.logconfig

import org.osgi.framework.{BundleActivator, BundleContext}
import org.openmole.tool.osgi.*

object Activator extends OSGiActivator {
  var context: Option[BundleContext] = None
}

class Activator extends BundleActivator {

  override def start(context: BundleContext) = {
    Activator.context = Some(context)
    // SLF4JBridgeHandler.install
    LoggerConfig.init
  }

  override def stop(context: BundleContext) = {
    Activator.context = None
  }

}