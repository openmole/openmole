/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.environment.gridscale

import java.io.InputStream

import org.openmole.core.communication.storage._
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, AccessControl }
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.file._

object LocalStorage:

  import gridscale.local.Local

  def home = Local.home()
  def child(parent: String, child: String) = (File(parent) / child).getAbsolutePath

  implicit def isStorage: StorageInterface[LocalStorage] & HierarchicalStorageInterface[LocalStorage] & EnvironmentStorage[LocalStorage] = new StorageInterface[LocalStorage] with HierarchicalStorageInterface[LocalStorage] with EnvironmentStorage[LocalStorage]:
    override def child(t: LocalStorage, parent: String, child: String)(using AccessControl.Priority): String = LocalStorage.child(parent, child)
    override def parent(t: LocalStorage, path: String)(using AccessControl.Priority): Option[String] = Option(File(path).getParent)
    override def name(t: LocalStorage, path: String): String = File(path).getName
    override def exists(t: LocalStorage, path: String)(using AccessControl.Priority): Boolean = t.accessControl { Local.exists(path) }
    override def list(t: LocalStorage, path: String)(using AccessControl.Priority): Seq[gridscale.ListEntry] = t.accessControl { Local.list(path) }
    override def makeDir(t: LocalStorage, path: String)(using AccessControl.Priority): Unit = t.accessControl { Local.makeDir(path) }
    override def rmDir(t: LocalStorage, path: String)(using AccessControl.Priority): Unit = t.accessControl { Local.rmDir(path) }
    override def rmFile(t: LocalStorage, path: String)(using AccessControl.Priority): Unit = t.accessControl { Local.rmFile(path) }

    override def upload(t: LocalStorage, src: File, dest: String, options: TransferOptions)(using AccessControl.Priority): Unit = t.accessControl:
      StorageInterface.upload(false, Local.writeFile)(src, dest, options)
    

    override def download(t: LocalStorage, src: String, dest: File, options: TransferOptions)(using AccessControl.Priority): Unit = t.accessControl:
      StorageInterface.download(false, Local.readFile)(src, dest, options)

    override def id(s: LocalStorage): String = s.id
    override def environment(s: LocalStorage): BatchEnvironment = s.environment
  

case class LocalStorage(accessControl: AccessControl, id: String, environment: BatchEnvironment, root: String)

