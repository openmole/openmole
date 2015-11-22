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

package org.openmole.plugin.environment.egi

import org.openmole.core.batch.storage._
import org.openmole.core.batch.control._
import org.openmole.core.workspace.Workspace
import org.openmole.tool.file._
import fr.iscpif.gridscale.storage.{ Storage ⇒ GSStorage, ListEntry, FileType }
import fr.iscpif.gridscale.egi.{ SRMLocation, GlobusAuthenticationProvider, SRMStorage, GlobusAuthentication }
import java.net.URI
import java.io.{ File, InputStream, OutputStream }
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.plugin.environment.gridscale.GridScaleStorage

import scala.sys.process.{ Process, ProcessLogger }

object EGIStorageService {

  def apply[A: GlobusAuthenticationProvider](s: SRMLocation, _environment: BatchEnvironment { def voName: String }, authentication: A) = new EGIStorageService {
    def threads = Workspace.preferenceAsInt(EGIEnvironment.LocalThreadsBySE)
    val usageControl = AvailabilityQuality(new LimitedAccess(threads, Workspace.preferenceAsInt(EGIEnvironment.MaxAccessesByMinuteSE)), Workspace.preferenceAsInt(EGIEnvironment.QualityHysteresis))
    val storage = SRMStorage(s.copy(basePath = ""), threads)(authentication)
    val url = new URI("srm", null, s.host, s.port, null, null, null)
    val remoteStorage = new LCGCpRemoteStorage(s.host, s.port, _environment.voName)
    val environment = _environment
    val root = s.basePath
    override lazy val id = new URI("srm", environment.voName, s.host, s.port, s.basePath, null, null).toString
  }

}

trait EGIStorageService extends PersistentStorageService with GridScaleStorage with CompressedTransfer {

  val usageControl: AvailabilityQuality
  import usageControl.quality

  override def exists(path: String)(implicit token: AccessToken): Boolean = quality { super.exists(path)(token) }
  override def listNames(path: String)(implicit token: AccessToken): Seq[String] = quality { super.listNames(path)(token) }
  override def list(path: String)(implicit token: AccessToken): Seq[ListEntry] = quality { super.list(path)(token) }
  override def makeDir(path: String)(implicit token: AccessToken): Unit = quality { super.makeDir(path)(token) }
  override def rmDir(path: String)(implicit token: AccessToken): Unit = quality { super.rmDir(path)(token) }
  override def rmFile(path: String)(implicit token: AccessToken): Unit = quality { super.rmFile(path)(token) }
  override def openInputStream(path: String)(implicit token: AccessToken): InputStream = quality { super.openInputStream(path)(token) }
  override def openOutputStream(path: String)(implicit token: AccessToken): OutputStream = quality { super.openOutputStream(path)(token) }
  override def upload(src: File, dest: String, options: TransferOptions)(implicit token: AccessToken) = quality { super.upload(src, dest, options)(token) }
  override def download(src: String, dest: File, options: TransferOptions)(implicit token: AccessToken) = quality { super.download(src, dest, options)(token) }
}

class LCGCpRemoteStorage(val host: String, val port: Int, val voName: String) extends RemoteStorage with LCGCp { s ⇒

  val timeout = Workspace.preferenceAsDuration(EGIEnvironment.RemoteTimeout).toSeconds

  @transient lazy val url = new URI("srm", null, host, port, null, null, null)

  protected def run(cmd: String) = {
    val output = new StringBuilder
    val error = new StringBuilder

    val logger =
      ProcessLogger(
        (o: String) ⇒ output.append("\n" + o),
        (e: String) ⇒ error.append("\n" + e)
      )

    val exit = Process(cmd) ! logger
    if (exit != 0) throw new RuntimeException(s"Command $cmd had a non 0 return value.\n Output: ${output.toString}. Error: ${error.toString}")
    output.toString
  }

  override def child(parent: String, child: String): String = GSStorage.child(parent, child)

  override def download(src: String, dest: File, options: TransferOptions): Unit =
    if (options.raw) download(src, dest)
    else Workspace.withTmpFile { tmpFile ⇒
      download(src, tmpFile)
      tmpFile.copyUncompressFile(dest)
    }

  private def download(src: String, dest: File): Unit = run(lcgCpCmd(url.resolve(src), dest.getAbsolutePath))

  override def upload(src: File, dest: String, options: TransferOptions): Unit =
    if (options.raw) upload(src, dest)
    else Workspace.withTmpFile { tmpFile ⇒
      src.copyCompressFile(tmpFile)
      upload(tmpFile, dest)
    }

  private def upload(src: File, dest: String): Unit = run(lcgCpCmd(src.getAbsolutePath, url.resolve(dest)))
}

