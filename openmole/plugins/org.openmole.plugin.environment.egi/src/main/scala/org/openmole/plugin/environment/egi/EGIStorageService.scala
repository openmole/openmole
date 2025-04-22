///*
// * Copyright (C) 2012 reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//
//import java.io.{ File, IOException, InputStream }
//import java.net.URI
//
//import fr.iscpif.gridscale.http.{ DPMWebDAVStorage, HTTPSAuthentication, WebDAVLocation }
//import fr.iscpif.gridscale.storage.{ ListEntry, Storage => GSStorage }
//import org.openmole.core.communication.storage.{ RemoteStorage, _ }
//import org.openmole.core.preference.Preference
//import org.openmole.core.threadprovider.ThreadProvider
//import org.openmole.core.workspace.{ NewFile, Workspace }
//import org.openmole.plugin.environment.batch.control._
//import org.openmole.plugin.environment.batch.environment.BatchEnvironment
//import org.openmole.plugin.environment.batch.storage._
//import org.openmole.plugin.environment.gridscale.GridScaleStorage
//import org.openmole.tool.file._
//import squants.time.Time
//
//import scala.sys.process.{ Process, ProcessLogger }
//import scala.util.Try
//
//trait EGIStorageService extends StorageService with GridScaleStorage with CompressedTransfer {
//  val accessControl: AvailabilityQuality
//  import accessControl.quality
//
//  override def exists(path: String)(implicit token: AccessToken): Boolean = quality { super.exists(path)(token) }
//  override def listNames(path: String)(implicit token: AccessToken): Seq[String] = quality { super.listNames(path)(token) }
//  override def list(path: String)(implicit token: AccessToken): Seq[ListEntry] = quality { super.list(path)(token) }
//  override def makeDir(path: String)(implicit token: AccessToken): Unit = quality { super.makeDir(path)(token) }
//  override def rmDir(path: String)(implicit token: AccessToken): Unit = quality { super.rmDir(path)(token) }
//  override def rmFile(path: String)(implicit token: AccessToken): Unit = quality { super.rmFile(path)(token) }
//  override def downloadStream(path: String, transferOptions: TransferOptions)(implicit token: AccessToken): InputStream = quality { super.downloadStream(path, transferOptions)(token) }
//  override def uploadStream(is: InputStream, path: String, transferOptions: TransferOptions)(implicit token: AccessToken) = quality { super.uploadStream(is, path, transferOptions)(token) }
//}
//

package org.openmole.plugin.environment.egi

import gridscale.egi.VOMS
import org.openmole.core.communication.storage.*
import org.openmole.core.workspace.TmpDirectory
import org.openmole.plugin.environment.batch.environment.AccessControl.Priority
import org.openmole.plugin.environment.batch.environment.{AccessControl, BatchEnvironment}
import org.openmole.plugin.environment.batch.storage.*
import org.openmole.tool.cache.TimeCache
import org.openmole.tool.file.*
//
//object NativeCommandCopy {
//
//  //def url: URI
//
//  //def child(parent: String, child: String): String = GSStorage.child(parent, child)
//
//  //  def download(src: String, dest: File, options: TransferOptions)(implicit newFile: NewFile): Unit =
//  //    try {
//  //      if (options.raw) download(src, dest)
//  //      else newFile.withTmpFile { tmpFile =>
//  //        download(src, tmpFile)
//  //        tmpFile.copyUncompressFile(dest)
//  //      }
//  //    }
//  //    catch {
//  //      case e: Throwable => throw new IOException(s"Error downloading $src to $dest from $url with option $options", e)
//  //    }
//
//  //private def download(src: String, dest: File): Unit = run(downloadCommand(url.resolve(src), dest.getAbsolutePath))
//
//  //  def upload(src: File, dest: String, options: TransferOptions)(implicit newFile: NewFile): Unit =
//  //    try {
//  //      if (options.raw) upload(src, dest)
//  //      else newFile.withTmpFile { tmpFile =>
//  //        src.copyCompressFile(tmpFile)
//  //        upload(tmpFile, dest)
//  //      }
//  //    }
//  //    catch {
//  //      case e: Throwable => throw new IOException(s"Error uploading $src to $dest from $url with option $options", e)
//  //    }
//
//  //private def upload(src: File, dest: String): Unit = run(uploadCommand(src.getAbsolutePath, url.resolve(dest)))
//
//}
//
//object EGIWebDAVStorageService {
//
//  def apply[A: HTTPSAuthentication](s: WebDAVLocation, _environment: BatchEnvironment, voName: String, debug: Boolean, authentication: A)(implicit preference: Preference, threadProvider: ThreadProvider, newFile: NewFile) = {
//    val storage =
//      new EGIWebDAVStorageService {
//        def threads = preference(EGIEnvironment.ConnectionsByWebDAVSE)
//
//        val accessControl = AvailabilityQuality(new LimitedAccess(threads, Int.MaxValue), preference(EGIEnvironment.QualityHysteresis))
//        val storage = DPMWebDAVStorage(s.copy(basePath = ""))(authentication)
//        val url = new URI("https", null, s.host, s.port, null, null, null)
//        val remoteStorage = new CurlRemoteStorage(s.host, s.port, voName, preference(EGIEnvironment.RemoteCopyTimeout), debug)
//        val environment = _environment
//        val root = s.basePath
//        val id = new URI("webdavs", voName, s.host, s.port, s.basePath, null, null).toString
//      }
//    StorageService.startGC(storage)
//    storage
//  }
//}
//
//trait EGIWebDAVStorageService <: EGIStorageService
//

//class CurlRemoteStorage(val host: String, val port: Int, val voName: String, val timeout: Time, val debug: Boolean) extends RemoteStorage with NativeCommandCopy { s =>
//  lazy val curl = new Curl(voName, debug, timeout)
//
//  @transient lazy val url = new URI("https", null, host, port, null, null, null)
//  def downloadCommand(from: URI, to: String): String = curl.download(from, to)
//  def uploadCommand(from: String, to: URI): String = curl.upload(from, to)
//
//  override def upload(src: File, dest: String, options: TransferOptions)(implicit newFile: NewFile): Unit =
//    try super.upload(src, dest, options)
//    catch {
//      case t: Throwable =>
//        Try(run(s"${curl.curl} -X DELETE ${url.resolve(dest)}"))
//        throw t
//    }
//}

import java.io.File
import java.net.URI

import org.openmole.core.communication.storage
import org.openmole.core.communication.storage.RemoteStorage
import org.openmole.core.workspace.TmpDirectory
import squants._
import java.net._

object CurlRemoteStorage {
  def run(cmd: String) =
    import scala.sys.process._

    val output = new StringBuilder
    val error = new StringBuilder

    val logger =
      ProcessLogger(
        (o: String) => output.append("\n" + o),
        (e: String) => error.append("\n" + e)
      )

    val exit = Process(Seq("bash", "-c", s"unset http_proxy; unset https_proxy; $cmd")) ! logger
    if (exit != 0) throw new RuntimeException(s"Command $cmd had a non 0 return value.\n Output: ${output.toString}. Error: ${error.toString}")
    output.toString

  case class Curl(voName: String, debug: Boolean, timeout: Time) {
    @transient lazy val curl =
      s"curl ${if (debug) "--verbose" else ""} --connect-timeout ${timeout.toSeconds.toInt.toString} --max-time ${timeout.toSeconds.toInt.toString} --cert $$X509_USER_PROXY --key $$X509_USER_PROXY --cacert $$X509_USER_PROXY --capath $$X509_CERT_DIR -f "

    def upload(from: String, to: String) = s"$curl -T $from -L $to"
    def download(from: String, to: String) = s"$curl -L $from -o $to"
    def delete(location: String) = s"""$curl -X "DELETE" $location"""
  }
}

case class CurlRemoteStorage(location: String, jobDirectory: String, voName: String, debug: Boolean, timeout: Time) extends RemoteStorage {

  //@transient lazy val url = new URI(location)
  @transient lazy val curl = CurlRemoteStorage.Curl(voName, debug, timeout)
  def resolve(dest: String) = gridscale.RemotePath.child(location, dest)

  override def upload(src: File, dest: Option[String], options: storage.TransferOptions)(implicit newFile: TmpDirectory): String =
    val uploadDestination = dest.getOrElse(child(jobDirectory, StorageSpace.timedUniqName))

    try
      try
        if options.raw
        then CurlRemoteStorage.run(curl.upload(src.getAbsolutePath, resolve(uploadDestination)))
        else
          newFile.withTmpFile: tmpFile =>
            src.copyCompressFile(tmpFile)
            CurlRemoteStorage.run(curl.upload(tmpFile.getAbsolutePath, resolve(uploadDestination)))
        uploadDestination
      catch
        case e: Throwable =>
          util.Try(CurlRemoteStorage.run(curl.delete(resolve(uploadDestination))))
          throw new java.io.IOException(s"Error uploading $src to $dest to $location with option $options", e)
    catch
      case t: Throwable =>
        util.Try(CurlRemoteStorage.run(s"${curl} -X DELETE ${resolve(uploadDestination)}"))
        throw t

  override def download(src: String, dest: File, options: storage.TransferOptions)(implicit newFile: TmpDirectory): Unit =
    try
      if options.raw
      then CurlRemoteStorage.run(curl.download(resolve(src), dest.getAbsolutePath))
      else
        newFile.withTmpFile: tmpFile =>
          CurlRemoteStorage.run(curl.download(resolve(src), tmpFile.getAbsolutePath))
          tmpFile.copyUncompressFile(dest)
    catch
      case e: Throwable => throw new java.io.IOException(s"Error downloading $src to $dest from $location with option $options", e)

  def child(parent: String, child: String): String = gridscale.RemotePath.child(parent, child)
}

object WebDavStorage:
  def id(s: WebDavStorage): String = s.url

  given (using _root_.gridscale.http.HTTP): HierarchicalStorageInterface[WebDavStorage] with
      def webdavServer(location: WebDavStorage) = gridscale.webdav.WebDAVSServer(location.url, location.proxyCache().factory)

      override def child(t: WebDavStorage, parent: String, child: String)(using AccessControl.Priority): String = gridscale.RemotePath.child(parent, child)
      override def parent(t: WebDavStorage, path: String)(using AccessControl.Priority): Option[String] = gridscale.RemotePath.parent(path)
      override def name(t: WebDavStorage, path: String): String = gridscale.RemotePath.name(path)

      override def exists(t: WebDavStorage, path: String)(using AccessControl.Priority): Boolean = t.accessControl { t.qualityControl { gridscale.webdav.exists(webdavServer(t), path) } }
      override def list(t: WebDavStorage, path: String)(using AccessControl.Priority): Seq[gridscale.ListEntry] = t.accessControl { t.qualityControl { gridscale.webdav.list(webdavServer(t), path) } }
      override def makeDir(t: WebDavStorage, path: String)(using AccessControl.Priority): Unit = t.accessControl { t.qualityControl { gridscale.webdav.mkDirectory(webdavServer(t), path) } }
      override def rmDir(t: WebDavStorage, path: String)(using AccessControl.Priority): Unit = t.accessControl { t.qualityControl { gridscale.webdav.rmDirectory(webdavServer(t), path) } }
      override def rmFile(t: WebDavStorage, path: String)(using AccessControl.Priority): Unit = t.accessControl { t.qualityControl { gridscale.webdav.rmFile(webdavServer(t), path) } }

      override def upload(t: WebDavStorage, src: File, dest: String, options: storage.TransferOptions)(using AccessControl.Priority): Unit = t.accessControl:
        t.qualityControl:
          StorageInterface.upload(true, gridscale.webdav.writeStream(webdavServer(t), _, _))(src, dest, options)
          //if (!exists(t, dest)) throw new InternalProcessingError(s"File $src has been successfully uploaded to $dest on $t but does not exist.")

      override def download(t: WebDavStorage, src: String, dest: File, options: storage.TransferOptions)(using AccessControl.Priority): Unit = t.accessControl:
        t.qualityControl:
          StorageInterface.download(true, gridscale.webdav.readStream[Unit](webdavServer(t), _, _))(src, dest, options)


      override def id(s: WebDavStorage): String = WebDavStorage.id(s)

case class WebDavStorage(url: String, accessControl: AccessControl, qualityControl: QualityControl, proxyCache: TimeCache[VOMS.VOMSCredential], environment: EGIEnvironment[_])