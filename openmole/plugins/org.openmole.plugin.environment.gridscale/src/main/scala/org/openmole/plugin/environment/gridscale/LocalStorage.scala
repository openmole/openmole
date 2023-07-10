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

object LocalStorage {

  import gridscale.effectaside._
  import gridscale.local

  def home(implicit interpreter: Effect[local.Local]) = interpreter().home()
  def child(parent: String, child: String) = (File(parent) / child).getAbsolutePath

  implicit def isStorage(implicit interpreter: Effect[local.Local]): StorageInterface[LocalStorage] with HierarchicalStorageInterface[LocalStorage] with EnvironmentStorage[LocalStorage] = new StorageInterface[LocalStorage] with HierarchicalStorageInterface[LocalStorage] with EnvironmentStorage[LocalStorage] {
    override def child(t: LocalStorage, parent: String, child: String): String = LocalStorage.child(parent, child)
    override def parent(t: LocalStorage, path: String): Option[String] = Option(File(path).getParent)
    override def name(t: LocalStorage, path: String): String = File(path).getName
    override def exists(t: LocalStorage, path: String): Boolean = t.accessControl { local.exists(path) }
    override def list(t: LocalStorage, path: String): Seq[gridscale.ListEntry] = t.accessControl { local.list(path) }
    override def makeDir(t: LocalStorage, path: String): Unit = t.accessControl { local.makeDir(path) }
    override def rmDir(t: LocalStorage, path: String): Unit = t.accessControl { local.rmDir(path) }
    override def rmFile(t: LocalStorage, path: String): Unit = t.accessControl { local.rmFile(path) }

    override def upload(t: LocalStorage, src: File, dest: String, options: TransferOptions): Unit = t.accessControl {
      StorageInterface.upload(false, local.writeFile(_, _))(src, dest, options)
    }

    override def download(t: LocalStorage, src: String, dest: File, options: TransferOptions): Unit = t.accessControl {
      StorageInterface.download(false, local.readFile[Unit](_, _))(src, dest, options)
    }

    override def id(s: LocalStorage): String = s.id
    override def environment(s: LocalStorage): BatchEnvironment = s.environment
  }

}

case class LocalStorage(accessControl: AccessControl, id: String, environment: BatchEnvironment, root: String)

