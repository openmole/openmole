/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.environment.batch

import java.io.File

import org.openmole.core.communication.storage._
import org.openmole.core.workspace.NewFile

package object storage {

  //  implicit class SimpleRemoteStorage[S](s: S)(implicit storage: Storage[S]) extends RemoteStorage {
  //    override def child(parent: String, child: String): String = storage.child(s, parent, child)
  //    
  //    override def download(src: String, dest: File, options: TransferOptions)(implicit newFile: NewFile) = storage.download(s, src, dest, options)
  //    override def upload(src: File, dest: String, options: TransferOptions)(implicit newFile: NewFile) = storage.upload(s, src, dest, options)
  //  }

}
