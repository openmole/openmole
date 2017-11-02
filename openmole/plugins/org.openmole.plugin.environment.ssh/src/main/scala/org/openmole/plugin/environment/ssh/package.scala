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

import freedsl.dsl._
import org.openmole.core.communication.storage.TransferOptions
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.dsl.File
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, Runtime, UsageControl }
import org.openmole.plugin.environment.batch.storage.{ StorageInterface, StorageService }
import org.openmole.plugin.environment.gridscale.{ LocalStorage, LogicalLinkStorage }
import simulacrum.typeclass
import squants.time.Time
import sun.util.resources.ro.CalendarData_ro

import scala.util.Try

package object ssh {
  //  class RemoteLogicalLinkStorage(val root: String) extends LogicalLinkStorage with SimpleStorage
  //  class RemoveLocalStorage(val root: String) extends LocalStorage with SimpleStorage

  @typeclass trait AsSSHServer[S] {
    def apply(s: S): _root_.gridscale.ssh.SSHServer
  }

  implicit def toSSHServer[S: AsSSHServer](s: S) = implicitly[AsSSHServer[S]].apply(s)

  import _root_.gridscale.{ ssh ⇒ gssh }

  implicit def isStorage[S: AsSSHServer](implicit interpreter: gssh.SSHInterpreter): StorageInterface[S] = new StorageInterface[S] {
    override def child(t: S, parent: String, child: String): String = _root_.gridscale.RemotePath.child(parent, child)
    override def parent(t: S, path: String): Option[String] = _root_.gridscale.RemotePath.parent(path)
    override def name(t: S, path: String): String = _root_.gridscale.RemotePath.name(path)
    override def home(t: S) = gssh.home[DSL](t).eval

    override def exists(t: S, path: String): Boolean = gssh.exists[DSL](t, path).eval
    override def list(t: S, path: String) = gssh.list[DSL](t, path).eval
    override def makeDir(t: S, path: String): Unit = gssh.makeDir[DSL](t, path).eval
    override def rmDir(t: S, path: String): Unit = gssh.rmDir[DSL](t, path).eval
    override def rmFile(t: S, path: String): Unit = gssh.rmFile[DSL](t, path).eval
    override def mv(t: S, from: String, to: String): Unit = gssh.mv[DSL](t, from, to).eval

    override def upload(t: S, src: File, dest: String, options: TransferOptions): Unit =
      StorageInterface.upload(false, gssh.writeFile[DSL](t, _, _).eval)(src, dest, options)
    override def download(t: S, src: String, dest: File, options: TransferOptions): Unit =
      StorageInterface.download(false, gssh.readFile[DSL, Unit](t, _, _).eval)(src, dest, options)
  }

  def localStorageService(environment: BatchEnvironment, concurrency: Int, root: String, sharedDirectory: Option[String])(implicit threadProvider: ThreadProvider, preference: Preference, localInterpreter: _root_.gridscale.local.LocalInterpreter) = {
    val remoteStorage = StorageInterface.remote(LogicalLinkStorage())
    def id = new URI("file", null, "localhost", -1, sharedDirectory.orNull, null, null).toString
    StorageService(LocalStorage(), root, id, environment, remoteStorage, concurrency, t ⇒ false)
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
  )(implicit storageInterface: StorageInterface[S], threadProvider: ThreadProvider, preference: Preference) = {

    val root = sharedDirectory match {
      case Some(p) ⇒ p
      case None ⇒
        val home = storageInterface.home(storage)
        storageInterface.child(storage, home, ".openmole/.tmp/ssh/")
    }

    implicit def logicalLinkStorage = LogicalLinkStorage.isStorage(_root_.gridscale.local.LocalInterpreter())
    implicit def localStorage = LocalStorage.isStorage(_root_.gridscale.local.LocalInterpreter())

    val remoteStorage = StorageInterface.remote(LogicalLinkStorage())
    if (storageSharedLocally) {
      def id = new URI("file", user, "localhost", -1, sharedDirectory.orNull, null, null).toString
      StorageService(LocalStorage(), root, id, environment, remoteStorage, concurrency, t ⇒ false)
    }
    else {
      def id = new URI("ssh", user, host, port, root, null, null).toString
      def isConnectionError(t: Throwable) = t match {
        case _: _root_.gridscale.ssh.ConnectionError ⇒ true
        case _: _root_.gridscale.authentication.AuthenticationException ⇒ true
        case _ ⇒ false
      }
      StorageService(storage, root, id, environment, remoteStorage, concurrency, isConnectionError)
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
    def ssh(frontend: _root_.gridscale.ssh.SSHServer)(implicit sshInterpreter: _root_.gridscale.ssh.SSHInterpreter, systemInterpreter: freedsl.system.SystemInterpreter): Frontend = new Frontend {
      override def run(command: String) =
        _root_.gridscale.ssh.run[DSL](frontend, command, verbose = true).tryEval
    }

    def ssh[A: _root_.gridscale.ssh.SSHAuthentication](host: String, port: Int, timeout: Time, authentication: A)(implicit sshInterpreter: _root_.gridscale.ssh.SSHInterpreter, systemInterpreter: freedsl.system.SystemInterpreter): Frontend = {
      val sshServer = _root_.gridscale.ssh.SSHServer(host, port, timeout)(authentication)
      ssh(sshServer)
    }

    def local(implicit localInterpreter: _root_.gridscale.local.LocalInterpreter) = new Frontend {
      override def run(command: String) = {
        _root_.gridscale.local.execute[DSL](command).tryEval
      }
    }

  }

  trait Frontend {
    def run(command: String): util.Try[_root_.gridscale.ExecutionResult]
  }

}
