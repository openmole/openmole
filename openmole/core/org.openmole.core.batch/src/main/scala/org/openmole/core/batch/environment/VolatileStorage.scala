/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.batch.environment

import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.batch.file.URIFile
import collection.JavaConversions._
import com.db4o.ObjectContainer
import java.net.URI
import org.openmole.misc.workspace.Workspace

class VolatileStorage(val environment: BatchEnvironment, val URI: URI, override val connections: Int) extends Storage {

  @transient override lazy val description = new ServiceDescription(Workspace.sessionUUID.toString)

  override def baseDir(token: AccessToken) = new URIFile(URI)
  override def persistentSpace(token: AccessToken)(implicit objectContainer: ObjectContainer): IURIFile = baseDir(token)
  override def tmpSpace(token: AccessToken): IURIFile = baseDir(token)
  def clean(token: AccessToken)(implicit objectContainer: ObjectContainer) = {}
}
