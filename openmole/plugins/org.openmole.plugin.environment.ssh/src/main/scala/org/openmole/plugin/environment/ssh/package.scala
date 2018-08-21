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
import org.openmole.plugin.environment.batch.environment.{BatchEnvironment, Runtime, AccessControl}
import org.openmole.plugin.environment.batch.storage._
import org.openmole.plugin.environment.gridscale.LocalStorage
import effectaside._
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.workspace.NewFile
import org.openmole.tool.cache.Lazy
import squants.Time

package object ssh {

  case class SSHStorage(sshServer: _root_.gridscale.ssh.SSHServer, accessControl: AccessControl, id: String, environment: BatchEnvironment, root: String)
  case class LocalStorageServe(localStorage: LocalStorage, accessControl: AccessControl, qualityControl: QualityControl)

  import _root_.gridscale.{ ssh ⇒ gssh }

  object SSHStorage {

    def home(sshServer: gssh.SSHServer)(implicit interpreter: Effect[gssh.SSH]) = gssh.home(sshServer)
    def child(parent: String, child: String) = _root_.gridscale.RemotePath.child(parent, child)

    implicit def isStorage(implicit interpreter: Effect[gssh.SSH]) = new StorageInterface[SSHStorage] with HierarchicalStorageInterface[SSHStorage] with EnvironmentStorage[SSHStorage] {
      implicit def toSSHServer(s: SSHStorage) = s.sshServer
      override def accessControl(s: SSHStorage): AccessControl = s.accessControl

      override def child(t: SSHStorage, parent: String, child: String): String = SSHStorage.child(parent, child)
      override def parent(t: SSHStorage, path: String): Option[String] = _root_.gridscale.RemotePath.parent(path)
      override def name(t: SSHStorage, path: String): String = _root_.gridscale.RemotePath.name(path)

      override def exists(t: SSHStorage, path: String): Boolean = gssh.exists(t, path)
      override def list(t: SSHStorage, path: String) = gssh.list(t, path)

      override def makeDir(t: SSHStorage, path: String): Unit = gssh.makeDir(t, path)
      override def rmDir(t: SSHStorage, path: String): Unit = gssh.rmDir(t, path)

      override def rmFile(t: SSHStorage, path: String): Unit = gssh.rmFile(t, path)

      override def upload(t: SSHStorage, src: File, dest: String, options: TransferOptions): Unit =
        StorageInterface.upload(false, gssh.writeFile(t, _, _))(src, dest, options)

      override def download(t: SSHStorage, src: String, dest: File, options: TransferOptions): Unit =
        StorageInterface.download(false, gssh.readFile[Unit](t, _, _))(src, dest, options)

      override def id(s: SSHStorage): String = s.id
      override def environment(s: SSHStorage): BatchEnvironment = s.environment
    }

    def isConnectionError(t: Throwable) = t match {
      case _: _root_.gridscale.ssh.ConnectionError ⇒ true
      case _: _root_.gridscale.authentication.AuthenticationException ⇒ true
      case _ ⇒ false
    }
  }

  def sshRoot[S](home: String, child: (String, String) ⇒ String, sharedDirectory: Option[String]) = {
    sharedDirectory match {
      case Some(p) ⇒ p
      case None    ⇒ child(home, ".openmole/.tmp/ssh/")
    }
  }

  def localStorage(
    environment:     BatchEnvironment,
    sharedDirectory: Option[String],
    accessControl:    AccessControl)(implicit local: Effect[_root_.gridscale.local.Local]) = {

    val root = sshRoot(LocalStorage.home, LocalStorage.child, sharedDirectory)
    def id = new URI("file", null, "localhost", -1, root, null, null).toString

    LocalStorage(accessControl, id, environment, root)
  }

  def localStorageSpace(local: LocalStorage)(implicit preference: Preference, replicaCatalog: ReplicaCatalog, interpreter: Effect[_root_.gridscale.local.Local]) = StorageSpace.hierarchicalStorageSpace(local, local.root, local.id, _ ⇒ false)

  def sshStorage(
    user:                 String,
    host:                 String,
    port:                 Int,
    sshServer:            _root_.gridscale.ssh.SSHServer,
    accessControl:         AccessControl,
    environment:          BatchEnvironment,
    sharedDirectory:      Option[String]
  )(implicit ssh: Effect[_root_.gridscale.ssh.SSH]) = {
    val root = sshRoot(SSHStorage.home(sshServer), SSHStorage.child, sharedDirectory)
    def id = new URI("ssh", user, host, port, root, null, null).toString
    SSHStorage(sshServer, accessControl, id, environment, root)
  }

  def sshStorageSpace(ssh: SSHStorage)(implicit preference: Preference, replicaCatalog: ReplicaCatalog, interpreter: Effect[_root_.gridscale.ssh.SSH]) = StorageSpace.hierarchicalStorageSpace(ssh, ssh.root, ssh.id, SSHStorage.isConnectionError)

  type LocalOrSSH = Either[(Any, LocalStorage), (Any, SSHStorage)]

  def getaccessControl(storage: LocalOrSSH) =
    storage match {
      case Left((_, s)) => s.accessControl
      case Right((_, s)) => s.accessControl
     }

  class RuntimeInstallation[S](
    frontend:       Frontend,
    storage: S,
    baseDirectory: String,
  )(implicit preference: Preference, newFile: NewFile, storageInterface: StorageInterface[S]) {

    val installMap = collection.mutable.Map[Runtime, String]()

    def apply(runtime: Runtime) = installMap.synchronized {
      def install =  SharedStorage.installRuntime(runtime, storage, frontend, baseDirectory)
      installMap.getOrElseUpdate(runtime, install)
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
