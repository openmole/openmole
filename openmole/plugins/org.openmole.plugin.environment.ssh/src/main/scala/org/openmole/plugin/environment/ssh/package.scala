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
package org.openmole.plugin.environment.ssh

import java.net.URI
import org.openmole.core.communication.storage.TransferOptions
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.preference.Preference
import org.openmole.core.workflow.dsl.{File, uniqName}
import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.core.workspace.TmpDirectory
import org.openmole.plugin.environment.batch.environment.{AccessControl, BatchEnvironment, BatchExecutionJob, BatchJobControl, Runtime, SerializedJob, UpdateInterval}
import org.openmole.plugin.environment.batch.storage.*
import org.openmole.plugin.environment.gridscale.{LocalStorage, LogicalLinkStorage}
import squants.Time
import org.openmole.tool.lock.*
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

import java.util.concurrent.locks.ReentrantLock

object SSHConnection:
  def defaultReconnect: OptionalArgument[Time] = None

case class SSHStorage(accessControl: AccessControl, id: String, environment: BatchEnvironment, root: String)(using val ssh: _root_.gridscale.ssh.SSH)
case class LocalStorageServer(localStorage: LocalStorage, accessControl: AccessControl, qualityControl: QualityControl)

import _root_.gridscale.{ssh => gssh}

object SSHStorage:

  def home(using gssh.SSH) = gssh.home()
  def child(parent: String, child: String) = _root_.gridscale.RemotePath.child(parent, child)

  given StorageInterface[SSHStorage] with HierarchicalStorageInterface[SSHStorage] with EnvironmentStorage[SSHStorage] with
    override def child(t: SSHStorage, parent: String, child: String)(using AccessControl.Priority): String = SSHStorage.child(parent, child)
    override def parent(t: SSHStorage, path: String)(using AccessControl.Priority): Option[String] = _root_.gridscale.RemotePath.parent(path)
    override def name(t: SSHStorage, path: String): String = _root_.gridscale.RemotePath.name(path)

    override def exists(t: SSHStorage, path: String)(using AccessControl.Priority): Boolean =
      import t.given
      t.accessControl { gssh.exists(path) }

    override def list(t: SSHStorage, path: String)(using AccessControl.Priority) =
      import t.given
      t.accessControl { gssh.list(path) }

    override def makeDir(t: SSHStorage, path: String)(using AccessControl.Priority): Unit =
      import t.given
      t.accessControl { gssh.makeDir(path) }

    override def rmDir(t: SSHStorage, path: String)(using AccessControl.Priority): Unit =
      import t.given
      t.accessControl { gssh.rmDir(path) }

    override def rmFile(t: SSHStorage, path: String)(using AccessControl.Priority): Unit =
      import t.given
      t.accessControl { gssh.rmFile(path) }

    override def upload(t: SSHStorage, src: File, dest: String, options: TransferOptions)(using AccessControl.Priority): Unit =
      import t.given
      try
        t.accessControl:
          StorageInterface.upload(false, gssh.writeFile)(src, dest, options)
      catch
        case e: Throwable => throw InternalProcessingError(s"Error uploading $src to $dest with options $options", e)

    override def download(t: SSHStorage, src: String, dest: File, options: TransferOptions)(using AccessControl.Priority): Unit =
      import t.given
      try
        t.accessControl:
          StorageInterface.download(false, gssh.readFile)(src, dest, options)
      catch
        case e: Throwable => throw InternalProcessingError(s"Error downloading $src to $dest with options $options", e)


    override def id(s: SSHStorage): String = s.id
    override def environment(s: SSHStorage): BatchEnvironment = s.environment


  def isConnectionError(t: Throwable) = t match
    case _: _root_.gridscale.ssh.ConnectionError ⇒ true
    case _: _root_.gridscale.authentication.AuthenticationException ⇒ true
    case _ ⇒ false


def sshRoot[S](home: String, child: (String, String) ⇒ String, sharedDirectory: Option[String]) =
  sharedDirectory match
    case Some(p) ⇒ p
    case None    ⇒ child(home, ".openmole/.tmp/ssh/")

def localStorage(
  environment:     BatchEnvironment,
  sharedDirectory: Option[String],
  accessControl:    AccessControl) =

  val root = sshRoot(LocalStorage.home, LocalStorage.child, sharedDirectory)
  def id = new URI("file", null, "localhost", -1, root, null, null).toString

  LocalStorage(accessControl, id, environment, root)

def localStorageSpace(local: LocalStorage)(using preference: Preference) =
  AccessControl.defaultPrirority:
    HierarchicalStorageSpace.create(local, local.root, local.id, _ ⇒ false)

def sshStorage(
  user:                 String,
  host:                 String,
  port:                 Int,
  accessControl:         AccessControl,
  environment:          BatchEnvironment,
  sharedDirectory:      Option[String]
)(using _root_.gridscale.ssh.SSH) =
  val root = sshRoot(SSHStorage.home, SSHStorage.child, sharedDirectory)
  def id = new URI("ssh", user, host, port, root, null, null).toString
  SSHStorage(accessControl, id, environment, root)

def sshStorageSpace(ssh: SSHStorage)(using preference: Preference) =
  AccessControl.defaultPrirority:
    HierarchicalStorageSpace.create(ssh, ssh.root, ssh.id, SSHStorage.isConnectionError)

case class RuntimeInstallation[S](
  frontend:       Frontend,
  storage:        S,
  baseDirectory: String)(using Preference, TmpDirectory, StorageInterface[S], HierarchicalStorageInterface[S]):

  val installMap = collection.mutable.Map[Runtime, String]()
  val lock = new ReentrantLock()

  def apply[S](runtime: Runtime) = lock:
    AccessControl.bypassAccessControl:
      def install =  SharedStorage.installRuntime(runtime, storage, frontend, baseDirectory)
      installMap.getOrElseUpdate(runtime, install)


object Frontend:
  def ssh(frontend: _root_.gridscale.ssh.SSHServer)(using _root_.gridscale.ssh.SSH): Frontend =
    command =>
      util.Try(_root_.gridscale.ssh.run(command, verbose = true))

  def ssh[A: _root_.gridscale.ssh.SSHAuthentication](host: String, port: Int, timeout: Time, authentication: A)(using _root_.gridscale.ssh.SSH): Frontend =
    val sshServer = _root_.gridscale.ssh.SSHServer(host, port, timeout)(authentication)
    ssh(sshServer)

  def local: Frontend =
    command => util.Try(_root_.gridscale.local.Local.execute(command))

trait Frontend:
  def run(command: String): util.Try[_root_.gridscale.ExecutionResult]

def submitToCluster[S: StorageInterface: HierarchicalStorageInterface: EnvironmentStorage, J](
  environment: BatchEnvironment,
  batchExecutionJob: BatchExecutionJob,
  storage: S,
  space: StorageSpace,
  submit: (SerializedJob, String, String, AccessControl.Priority) => J,
  state: (J, AccessControl.Priority) => ExecutionState,
  delete: (J, AccessControl.Priority) => Unit,
  stdOutErr: (J, AccessControl.Priority) => (String, String),
  refresh: Option[Time] = None)(using services: BatchEnvironment.Services, priority: AccessControl.Priority) =
  import services.*

  val jobDirectory = HierarchicalStorageSpace.createJobDirectory(storage, space)
  val remoteStorage = LogicalLinkStorage.remote(LogicalLinkStorage(), jobDirectory)

  def clean(priority: AccessControl.Priority) =
    given AccessControl.Priority = priority
    StorageService.rmDirectory(storage, jobDirectory)

  try
    def replicate(f: File, options: TransferOptions) =
      BatchEnvironment.toReplicatedFile(
        StorageService.uploadInDirectory(storage, _, space.replicaDirectory, _),
        StorageService.exists(storage, _),
        StorageService.rmFile(storage, _, background = true),
        environment,
        StorageService.id(storage)
      )(f, options)

    def upload(f: File, options: TransferOptions) = StorageService.uploadInDirectory(storage, f, jobDirectory, options)

    val sj = BatchEnvironment.serializeJob(environment, batchExecutionJob, remoteStorage, replicate, upload, StorageService.id(storage))
    val outputPath = StorageService.child(storage, jobDirectory, uniqName("job", ".out"))

    val job = submit(sj, outputPath, jobDirectory, priority)

    def download(src: String, dest: File, options: TransferOptions, priority: AccessControl.Priority) =
      given AccessControl.Priority = priority
      StorageService.download(storage, src, dest, options)

    BatchJobControl(
      () => refresh.map(UpdateInterval.fixed) getOrElse BatchEnvironment.defaultUpdateInterval(services.preference),
      () => StorageService.id(storage),
      priority => state(job, priority),
      priority ⇒ delete(job, priority),
      priority ⇒ stdOutErr(job, priority),
      download,
      () ⇒ outputPath,
      priority ⇒ clean(priority)
    )
  catch
    case t: Throwable =>
      util.Try(clean)
      throw t


def cleanSSHStorage(storage: Either[(StorageSpace, LocalStorage), (StorageSpace, SSHStorage)], background: Boolean)(using services: BatchEnvironment.Services, s: _root_.gridscale.ssh.SSH, priority: AccessControl.Priority) =
  storage match
    case Left((space, local)) ⇒ HierarchicalStorageSpace.clean(local, space,background)
    case Right((space, ssh))  ⇒ HierarchicalStorageSpace.clean(ssh, space, background)


object SSHProxy:
  def toSSHServer(a: Authenticated, timeout: Time) =
    _root_.gridscale.ssh.SSHServer(a.proxy.host, a.proxy.port, timeout = timeout)(a.authentication)

  case class Authenticated(proxy: SSHProxy, authentication: SSHAuthentication)

  def authenticated(proxy: SSHProxy)(using AuthenticationStore, Cypher) =
    val authentication = SSHAuthentication.find(proxy.user, proxy.host, proxy.port)
    Authenticated(proxy, authentication)

  def apply(
    user: String,
    host: String,
    port: Int = 22) =
    new SSHProxy(user, host, port)

case class SSHProxy(user: String, host: String, port: Int)