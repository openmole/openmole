/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.batch.environment

import com.db4o.ObjectContainer
import java.net.URI
import org.openmole.misc.tools.service.Logger
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.StorageControl
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.control.QualityControl
import org.openmole.core.batch.file.RelativePath
import org.openmole.core.batch.file.IURIFile

import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._

object Storage extends Logger

trait Storage extends BatchService {

  @transient lazy val description = new ServiceDescription(URI)

  StorageControl.register(description, new QualityControl(Workspace.preferenceAsInt(BatchEnvironment.QualityHysteresis)))

  import Storage._

  def URI: URI
  def persistentSpace(token: AccessToken)(implicit objectContainer: ObjectContainer): IURIFile
  def tmpSpace(token: AccessToken): IURIFile
  def baseDir(token: AccessToken): IURIFile
  def root =
    if (URI.getScheme != null) new URI(URI.getScheme + "://" + URI.getAuthority)
    else new URI(URI.getScheme + ":/")
  def resolve(path: String) = root.resolve(path)

  def path = new RelativePath(root)

  def clean(token: AccessToken)(implicit objectContainer: ObjectContainer)

  override def toString: String = URI.toString
}
