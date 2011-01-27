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

package org.openmole.misc.workspace.internal

import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout

import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.openmole.misc.workspace.IWorkspace
import org.openmole.commons.tools.io.FileUtil
import org.openmole.commons.tools.service.OSGiActivator

object Activator extends OSGiActivator {
  var context: Option[BundleContext] = None
}

class Activator extends BundleActivator {
  
  private var reg: Option[ServiceRegistration] = None
  private var workspace: Option[Workspace] = None
  
  override def start(context: BundleContext) = {
    Activator.context = Some(context)
    val logger = Logger.getLogger("org.apache.commons.configuration.ConfigurationUtils")
    logger.addAppender(new ConsoleAppender(new PatternLayout("%-5p %d  %c - %F:%L - %m%n")))
    logger.setLevel(Level.WARN)
    workspace = Some(new Workspace)
    reg = Some(context.registerService(classOf[IWorkspace].getName, workspace.get, null))
  }

  override def stop(context: BundleContext) = {
    reg.foreach{_.unregister}
    Activator.context = None
    workspace.foreach{w => FileUtil.recursiveDelete(w.tmpDir.getLocation)}
    workspace = None
  }
  
}
