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

import fr.iscpif.gridscale.http.{HTTPSAuthentication, WebDAVLocation, DPMWebDAVStorage}
import org.openmole.core.batch.storage._
import org.openmole.core.batch.control._
import org.openmole.core.workspace.Workspace
import org.openmole.tool.file._
import fr.iscpif.gridscale.storage.{ Storage ⇒ GSStorage, ListEntry, FileType }
import fr.iscpif.gridscale.egi.{ SRMLocation, GlobusAuthenticationProvider, SRMStorage }
import java.net.URI
import java.io.{IOException, File, InputStream, OutputStream}
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.plugin.environment.gridscale.GridScaleStorage

import scala.sys.process.{ Process, ProcessLogger }
import scala.util.Try

trait EGIStorageService extends PersistentStorageService with GridScaleStorage with CompressedTransfer {
  val usageControl: AvailabilityQuality
  import usageControl.quality

  override def exists(path: String)(implicit token: AccessToken): Boolean = quality { super.exists(path)(token) }
  override def listNames(path: String)(implicit token: AccessToken): Seq[String] = quality { super.listNames(path)(token) }
  override def list(path: String)(implicit token: AccessToken): Seq[ListEntry] = quality { super.list(path)(token) }
  override def makeDir(path: String)(implicit token: AccessToken): Unit = quality { super.makeDir(path)(token) }
  override def rmDir(path: String)(implicit token: AccessToken): Unit = quality { super.rmDir(path)(token) }
  override def rmFile(path: String)(implicit token: AccessToken): Unit = quality { super.rmFile(path)(token) }
  override def downloadStream(path: String, transferOptions: TransferOptions)(implicit token: AccessToken): InputStream = quality { super.downloadStream(path, transferOptions)(token) }
  override def uploadStream(is: InputStream, path: String, transferOptions: TransferOptions)(implicit token: AccessToken) = quality { super.uploadStream(is, path, transferOptions)(token) }
}

object EGISRMStorageService {

  def apply[A: GlobusAuthenticationProvider](s: SRMLocation, _environment: BatchEnvironment, voName: String, authentication: A) = new EGISRMStorageService {
    def threads = Workspace.preferenceAsInt(EGIEnvironment.ConnectionsBySRMSE)
    val usageControl = AvailabilityQuality(new LimitedAccess(threads, Int.MaxValue), Workspace.preferenceAsInt(EGIEnvironment.QualityHysteresis))
    val storage = SRMStorage(s.copy(basePath = ""), threads)(authentication)
    val url = new URI("srm", null, s.host, s.port, null, null, null)
    val remoteStorage = new LCGCpRemoteStorage(s.host, s.port, voName)
    val environment = _environment
    val root = s.basePath
    override lazy val id = new URI("srm", voName, s.host, s.port, s.basePath, null, null).toString
  }

}

trait EGISRMStorageService <: EGIStorageService

trait NativeCommandCopy {

  def url: URI

  protected def run(cmd: String) = {
    val output = new StringBuilder
    val error = new StringBuilder

    val logger =
      ProcessLogger(
        (o: String) ⇒ output.append("\n" + o),
        (e: String) ⇒ error.append("\n" + e)
      )

    val exit = Process(Seq("bash", "-c", cmd)) ! logger
    if (exit != 0) throw new RuntimeException(s"Command $cmd had a non 0 return value.\n Output: ${output.toString}. Error: ${error.toString}")
    output.toString
  }

  def child(parent: String, child: String): String = GSStorage.child(parent, child)

  def download(src: String, dest: File, options: TransferOptions): Unit =
    try {
      if (options.raw) download(src, dest)
      else Workspace.withTmpFile { tmpFile ⇒
        download(src, tmpFile)
        tmpFile.copyUncompressFile(dest)
      }
    } catch {
      case e: Throwable => throw new IOException(s"Error downloading $src to $dest from $url with option $options", e)
    }

  private def download(src: String, dest: File): Unit = run(downloadCommand(url.resolve(src), dest.getAbsolutePath))

  def upload(src: File, dest: String, options: TransferOptions): Unit =
    try {
      if (options.raw) upload(src, dest)
      else Workspace.withTmpFile { tmpFile ⇒
        src.copyCompressFile(tmpFile)
        upload(tmpFile, dest)
      }
    } catch {
      case e: Throwable => throw new IOException(s"Error uploading $src to $dest from $url with option $options", e)
    }


  private def upload(src: File, dest: String): Unit = run(uploadCommand(src.getAbsolutePath, url.resolve(dest)))

  def downloadCommand(from: URI, to: String): String
  def uploadCommand(from: String, to: URI): String
}

class LCGCpRemoteStorage(val host: String, val port: Int, val voName: String) extends RemoteStorage with NativeCommandCopy { s ⇒
  lazy val lcgcp = new LCGCp(voName)

  @transient lazy val url = new URI("srm", null, host, port, null, null, null)
  def downloadCommand(from: URI, to: String): String = lcgcp.download(from, to)
  def uploadCommand(from: String, to: URI): String = lcgcp.upload(from, to)
}

object EGIWebDAVStorageService {

  def apply[A: HTTPSAuthentication](s: WebDAVLocation, _environment: BatchEnvironment, voName: String, debug: Boolean, authentication: A) = new EGIWebDAVStorageService {
    def threads = Workspace.preferenceAsInt(EGIEnvironment.ConnectionsByWebDAVSE)
    val usageControl = AvailabilityQuality(new LimitedAccess(threads, Int.MaxValue), Workspace.preferenceAsInt(EGIEnvironment.QualityHysteresis))
    val storage = DPMWebDAVStorage(s.copy(basePath = ""))(authentication)
    val url = new URI("https", null, s.host, s.port, null, null, null)
    val remoteStorage = new CurlRemoteStorage(s.host, s.port, voName, debug)
    val environment = _environment
    val root = s.basePath
    override lazy val id = new URI("webdavs", voName, s.host, s.port, s.basePath, null, null).toString
  }

}

trait EGIWebDAVStorageService <: EGIStorageService /*{
  override def upload(src: File, dest: String, options: TransferOptions)(implicit token: AccessToken): Unit = {
    super.upload(src, dest, options)
    val is =
      try openInputStream(dest)
      catch {
        case e: Throwable => throw new IOException(s"File upload failed from $src to $dest")
      }
    is.close
  }
}*/

class CurlRemoteStorage(val host: String, val port: Int, val voName: String, val debug: Boolean) extends RemoteStorage with NativeCommandCopy { s ⇒
  lazy val curl = new Curl(voName, debug)
  @transient lazy val url = new URI("https", null, host, port, null, null, null)
  def downloadCommand(from: URI, to: String): String = curl.download(from, to)
  def uploadCommand(from: String, to: URI): String = curl.upload(from, to)

  override def upload(src: File, dest: String, options: TransferOptions): Unit =
    try super.upload(src, dest, options)
    catch {
      case t: Throwable =>
        Try(run(s"${curl.curl} -X DELETE ${url.resolve(dest)}"))
        throw t
    }
}