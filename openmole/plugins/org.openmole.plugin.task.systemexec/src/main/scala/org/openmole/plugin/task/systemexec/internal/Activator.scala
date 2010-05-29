/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.systemexec.internal

import org.openmole.misc.workspace.IWorkspace;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.openmole.commons.aspect.caching.SoftCachable

object Activator extends BundleActivator {

  var context: BundleContext = null
 
  override def start(context: BundleContext) = {
    this.context = context
  }

  override def stop(context: BundleContext) = {
    this.context = null
  }

  @SoftCachable
  def getWorkspace: IWorkspace = {
    def ref = context.getServiceReference(classOf[IWorkspace].getName)
    return context.getService(ref) match {
      case ws: IWorkspace => ws
      case _ => throw new ClassCastException
    }
  }

}
