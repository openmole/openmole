/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.tools.service

import org.openmole.misc.exception.InternalProcessingError
import org.osgi.framework.BundleContext

trait OSGiActivator {
  def context: Option[BundleContext]
  def contextOrException = context.getOrElse(throw new InternalProcessingError("Context uninitialized"))
  
  def getService[T](interface: Class[T]): T = {
    val ctx = contextOrException
    val ref = ctx.getServiceReference(interface.getName)
    ctx.getService(ref).asInstanceOf[T]
  }
  
  def enabled = context.isDefined
}
