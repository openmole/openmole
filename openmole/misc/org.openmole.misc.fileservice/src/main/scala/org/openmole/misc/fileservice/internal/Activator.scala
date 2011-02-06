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

package org.openmole.misc.fileservice.internal

import org.openmole.commons.tools.service.OSGiActivator

import org.openmole.misc.fileservice.IFileService
import org.openmole.misc.hashservice.IHashService
import org.openmole.misc.updater.IUpdater
import org.openmole.misc.workspace.IWorkspace
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration

object Activator extends OSGiActivator {
  var context: Option[BundleContext] = None
  
  lazy val workspace = getService(classOf[IWorkspace])
  lazy val hashService = getService(classOf[IHashService])
  lazy val updater = getService(classOf[IUpdater])
}

class Activator extends BundleActivator {

  var msgSerial: ServiceRegistration = null

  override def start(context: BundleContext) = {
    Activator.context = Some(context)
    msgSerial = context.registerService(classOf[IFileService].getName, FileService, null)
  }

  override def stop(context: BundleContext) = {
    Activator.context = None
    msgSerial.unregister
  }
  
}