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
import org.openmole.plugin.environment.batch.storage.{ StorageInterface, StorageService, StorageSpace }
import org.openmole.plugin.environment.gridscale.{ LocalStorage, LogicalLinkStorage }
import squants.time.Time
import effectaside._
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.tool.cache.Lazy

package object ssh {
  //  class RemoteLogicalLinkStorage(val root: String) extends LogicalLinkStorage with SimpleStorage
  //  class RemoveLocalStorage(val root: String) extends LocalStorage with SimpleStorage

  trait AsSSHServer[S] {
    def apply(s: S): _root_.gridscale.ssh.SSHServer
  }

  implicit def toSSHServer[S: AsSSHServer](s: S) = implicitly[AsSSHServer[S]].apply(s)

  import _root_.gridscale.{ ssh ⇒ gssh }

  implicit def isStorage[S: AsSSHServer](implicit interpreter: Effect[gssh.SSH]): StorageInterface[S] = new StorageInterface[S] {
    override def child(t: S, parent: String, child: String): String = _root_.gridscale.RemotePath.child(parent, child)
    override def parent(t: S, path: String): Option[String] = _root_.gridscale.RemotePath.parent(path)
    override def name(t: S, path: String): String = _root_.gridscale.RemotePath.name(path)
    override def home(t: S) = gssh.home(t)

    override def exists(t: S, path: String): Boolean = gssh.exists(t, path)
    override def list(t: S, path: String) = gssh.list(t, path)
    override def makeDir(t: S, path: String): Unit = gssh.makeDir(t, path)
    override def rmDir(t: S, path: String): Unit = gssh.rmDir(t, path)
    override def rmFile(t: S, path: String): Unit = gssh.rmFile(t, path)
    override def mv(t: S, from: String, to: String): Unit = gssh.mv(t, from, to)

    override def upload(t: S, src: File, dest: String, options: TransferOptions): Unit =
      StorageInterface.upload(false, gssh.writeFile(t, _, _))(src, dest, options)
    override def download(t: S, src: String, dest: File, options: TransferOptions): Unit =
      StorageInterface.download(false, gssh.readFile[Unit](t, _, _))(src, dest, options)
  }

  def localStorageService(
    environment:     BatchEnvironment,
    concurrency:     Int,
    root:            String,
    sharedDirectory: Option[String])(implicit threadProvider: ThreadProvider, preference: Preference, replicaCatalog: ReplicaCatalog, localInterpreter: Effect[_root_.gridscale.local.Local]) = {
    def id = new URI("file", null, "localhost", -1, sharedDirectory.orNull, null, null).toString
    val storage = LocalStorage()
    def storageSpace = StorageSpace.hierarchicalStorageSpace(storage, root, id, _ ⇒ false)

    StorageService(storage, id, environment, concurrency, Lazy(storageSpace))
  }

  def sshStorageService[S](
    user:                 String,
    host:                 String,
    port:                 Int,
    storage:              S,
    concurrency:          Int,
    environment:          BatchEnvironment,
    sharedDirectory:      Option[String],
    storageSharedLocally: Boolean
  )(implicit storageInterface: StorageInterface[S], threadProvider: ThreadProvider, preference: Preference, replicaCatalog: ReplicaCatalog) = {

    val root = sharedDirectory match {
      case Some(p) ⇒ p
      case None ⇒
        val home = storageInterface.home(storage)
        storageInterface.child(storage, home, ".openmole/.tmp/ssh/")
    }

    if (storageSharedLocally) {
      def id = new URI("file", user, "localhost", -1, sharedDirectory.orNull, null, null).toString

      val storage = LocalStorage()
      def storageSpace = StorageSpace.hierarchicalStorageSpace(storage, root, id, _ ⇒ false)

      StorageService(storage, id, environment, concurrency, Lazy(storageSpace))
    }
    else {
      def id = new URI("ssh", user, host, port, root, null, null).toString
      def isConnectionError(t: Throwable) = t match {
        case _: _root_.gridscale.ssh.ConnectionError ⇒ true
        case _: _root_.gridscale.authentication.AuthenticationException ⇒ true
        case _ ⇒ false
      }
      def storageSpace = StorageSpace.hierarchicalStorageSpace(storage, root, id, isConnectionError)

      StorageService(storage, id, environment, concurrency, Lazy(storageSpace))
    }
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
