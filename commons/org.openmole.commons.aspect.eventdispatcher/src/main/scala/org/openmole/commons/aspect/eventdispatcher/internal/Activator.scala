/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
 */

package org.openmole.commons.aspect.eventdispatcher.internal

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.openmole.commons.aspect.eventdispatcher.IEventDispatcher

object Activator {
  var context: Option[BundleContext] = None
  
  lazy val eventDispatcher = new EventDispatcher
}

class Activator extends BundleActivator {

  private var reg: ServiceRegistration = null

  override def start(bc: BundleContext) = {
    Activator.context = Some(bc)
    reg = bc.registerService(classOf[IEventDispatcher].getName, Activator.eventDispatcher, null)
  }

  override def stop(bc: BundleContext) = reg.unregister

}
