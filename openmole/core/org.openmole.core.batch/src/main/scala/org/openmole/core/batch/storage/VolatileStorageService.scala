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

package org.openmole.core.batch.storage

import org.openmole.core.batch.control._
import org.openmole.misc.workspace._
import collection.JavaConversions._
import com.db4o.ObjectContainer

trait VolatileStorageService extends StorageService { this: Storage ⇒
  def persistentDir(implicit token: AccessToken, objectContainer: ObjectContainer) = baseDir(token)
  def tmpDir(implicit token: AccessToken) = baseDir(token)
  def clean(implicit token: AccessToken, objectContainer: ObjectContainer) = {}
}
