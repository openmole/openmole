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

package org.openmole.plugin.environment.ssh

//import java.io.File
//import java.net.URI
//
//import org.openmole.core.communication.storage.RemoteStorage
//import org.openmole.plugin.environment.batch.control.{ UnlimitedAccess, UsageControl }
//import org.openmole.plugin.environment.batch.environment.BatchEnvironment
//import org.openmole.plugin.environment.batch.storage._
//import org.openmole.plugin.environment.gridscale.LogicalLinkStorage

//trait SSHPersistentStorage <: BatchEnvironment with SSHAccess { st ⇒
//
//  type SS = StorageService
//
//  def user: String
//  def host: String
//  def port: Int
//
//  def usageControl: UsageControl
//  def sharedDirectory: Option[String]
//
//  trait StorageRoot <: Storage {
//    def home: String
//    lazy val root = sharedDirectory match {
//      case Some(p) ⇒ p
//      case None    ⇒ child(home, ".openmole/.tmp/ssh/")
//    }
//  }
//
//  def storageSharedLocally: Boolean
//
//  lazy val storage = {
//    val storage =
//      storageSharedLocally match {
//        case true ⇒
//          new StorageService with LogicalLinkStorage with StorageRoot {
//            def usageControl = UnlimitedAccess
//            lazy val remoteStorage: RemoteStorage = new RemoteLogicalLinkStorage(root)
//            def url = new URI("file", st.user, "localhost", -1, sharedDirectory.orNull, null, null)
//            def id: String = url.toString
//            val environment = st
//            override def parent(path: String) =
//              Option(new File(path).getCanonicalFile.getParentFile).map(_.getAbsolutePath)
//          }
//        case false ⇒
//          new StorageService with SSHStorageService with StorageRoot {
//            def usageControl = st.usageControl
//            val environment = st
//            def id = new URI("ssh", st.user, st.host, st.port, sharedDirectory.orNull, null, null).toString
//            def host: String = environment.host
//            def credential = environment.credential
//            def user: String = environment.user
//            def port: Int = environment.port
//          }
//      }
//
//    StorageService.startGC(storage)(services.threadProvider, services.preference)
//    storage
//  }
//
//}
