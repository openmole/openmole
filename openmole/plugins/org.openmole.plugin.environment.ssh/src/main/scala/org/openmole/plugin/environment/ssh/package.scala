/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.plugin.environment

import java.net.URI

import org.openmole.core.communication.storage.TransferOptions
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.dsl.File
import org.openmole.plugin.environment.batch.environment.{ AccessToken, BatchEnvironment, Runtime, UsageControl }
import org.openmole.plugin.environment.batch.storage._
import org.openmole.plugin.environment.gridscale.{ LocalStorage, LogicalLinkStorage }
import squants.time.Time
import effectaside._
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.tool.cache.Lazy
import squants.Time

package object ssh {

  case class SSHStorageServer(sshServer: _root_.gridscale.ssh.SSHServer, usageControl: UsageControl, qualityControl: QualityControl)

  import _root_.gridscale.{ ssh ⇒ gssh }

  object SSHStorageServer {
    implicit def isStorage(implicit interpreter: Effect[gssh.SSH]) = new StorageInterface[SSHStorageServer] with HierarchicalStorageInterface[SSHStorageServer] {
      implicit def toSSHServer(s: SSHStorageServer) = s.sshServer
      override def quality(s: SSHStorageServer): QualityControl = s.qualityControl
      override def usageControl(s: SSHStorageServer): UsageControl = s.usageControl

      override def child(t: SSHStorageServer, parent: String, child: String): String = _root_.gridscale.RemotePath.child(parent, child)
      override def parent(t: SSHStorageServer, path: String): Option[String] = _root_.gridscale.RemotePath.parent(path)
      override def name(t: SSHStorageServer, path: String): String = _root_.gridscale.RemotePath.name(path)

      override def home(t: SSHStorageServer) = gssh.home(t)
      override def exists(t: SSHStorageServer, path: String): Boolean = gssh.exists(t, path)
      override def list(t: SSHStorageServer, path: String) = gssh.list(t, path)

      override def makeDir(t: SSHStorageServer, path: String): Unit = gssh.makeDir(t, path)
      override def rmDir(t: SSHStorageServer, path: String): Unit = gssh.rmDir(t, path)

      override def rmFile(t: SSHStorageServer, path: String): Unit = gssh.rmFile(t, path)

      override def upload(t: SSHStorageServer, src: File, dest: String, options: TransferOptions): Unit =
        StorageInterface.upload(false, gssh.writeFile(t, _, _))(src, dest, options)

      override def download(t: SSHStorageServer, src: String, dest: File, options: TransferOptions): Unit =
        StorageInterface.download(false, gssh.readFile[Unit](t, _, _))(src, dest, options)
    }
  }

  def localStorageService(
    storage:         LocalStorage,
    environment:     BatchEnvironment,
    sharedDirectory: Option[String])(implicit threadProvider: ThreadProvider, preference: Preference, replicaCatalog: ReplicaCatalog) = {

    val root = sshRoot(storage, sharedDirectory)
    def id = new URI("file", null, "localhost", -1, root, null, null).toString
    def storageSpace = StorageSpace.hierarchicalStorageSpace(storage, root, id, _ ⇒ false)

    StorageService(storage, id, environment, Lazy(storageSpace))
  }

  def sshRoot[S](s: S, sharedDirectory: Option[String])(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S]) = {
    sharedDirectory match {
      case Some(p) ⇒ p
      case None ⇒
        val home = hierarchicalStorageInterface.home(s)
        storageInterface.child(s, home, ".openmole/.tmp/ssh/")
    }
  }

  def sshStorageService[S](
    user:                 String,
    host:                 String,
    port:                 Int,
    storage:              S,
    environment:          BatchEnvironment,
    sharedDirectory:      Option[String],
    storageSharedLocally: Boolean
  )(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], threadProvider: ThreadProvider, preference: Preference, replicaCatalog: ReplicaCatalog) = {
    val root = sshRoot(storage, sharedDirectory)

    def id = new URI("ssh", user, host, port, root, null, null).toString
    def isConnectionError(t: Throwable) = t match {
      case _: _root_.gridscale.ssh.ConnectionError ⇒ true
      case _: _root_.gridscale.authentication.AuthenticationException ⇒ true
      case _ ⇒ false
    }
    def storageSpace = StorageSpace.hierarchicalStorageSpace(storage, root, id, isConnectionError)

    StorageService(storage, id, environment, Lazy(storageSpace))
  }

  class RuntimeInstallation(
    frontend:       Frontend,
    storageService: StorageService[_]
  )(implicit services: BatchEnvironment.Services) {

    val installMap = collection.mutable.Map[Runtime, String]()

    def apply(runtime: Runtime) = installMap.synchronized {
      installMap.get(runtime) match {
        case Some(p) ⇒ p
        case None ⇒
          import services._
          val p = SharedStorage.installRuntime(runtime, storageService, frontend)
          installMap.put(runtime, p)
          p
      }
    }
  }

  object Frontend {
    def ssh(frontend: _root_.gridscale.ssh.SSHServer)(implicit ssh: Effect[_root_.gridscale.ssh.SSH], system: Effect[System]): Frontend = new Frontend {
      override def run(command: String) =
        util.Try(_root_.gridscale.ssh.run(frontend, command, verbose = true))
    }

    def ssh[A: _root_.gridscale.ssh.SSHAuthentication](host: String, port: Int, timeout: Time, authentication: A)(implicit sshEffect: Effect[_root_.gridscale.ssh.SSH], system: Effect[System]): Frontend = {
      val sshServer = _root_.gridscale.ssh.SSHServer(host, port, timeout)(authentication)
      ssh(sshServer)
    }

    def local(implicit local: Effect[_root_.gridscale.local.Local]) = new Frontend {
      override def run(command: String) =
        util.Try(_root_.gridscale.local.execute(command))
    }

  }

  trait Frontend {
    def run(command: String): util.Try[_root_.gridscale.ExecutionResult]
  }

}
