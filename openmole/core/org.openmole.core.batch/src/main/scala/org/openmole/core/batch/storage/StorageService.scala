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

package org.openmole.core.batch.storage

import java.net.URI
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.refresh._
import org.openmole.core.serializer._
import org.openmole.misc.filedeleter._
import org.openmole.misc.workspace._
import com.db4o.ObjectContainer
import fr.iscpif.gridscale.storage.FileType
import java.io._

trait StorageService extends BatchService with Storage {

  def remoteStorage: Storage
  def clean(implicit token: AccessToken, objectContainer: ObjectContainer)

  def url: URI
  lazy val id = url.toString

  @transient lazy val serializedRemoteStorage = {
    val file = Workspace.newFile("remoteStorage", ".xml")
    FileDeleter.deleteWhenGarbageCollected(file)
    SerializerService.serializeAndArchiveFiles(remoteStorage, file)
    file
  }

  @transient protected var baseSpaceVar: Option[String] = None

  def persistentDir(implicit token: AccessToken, objectContainer: ObjectContainer): String
  def tmpDir(implicit token: AccessToken): String
  def baseDir(implicit token: AccessToken): String = synchronized {
    baseSpaceVar match {
      case Some(s) ⇒ s
      case None ⇒
        val basePath = mkBaseDir
        baseSpaceVar = Some(basePath)
        basePath
    }
  }

  protected def mkBaseDir(implicit token: AccessToken): String = synchronized {
    val rootFile = new File(root)
    val baseDir = new File(rootFile, baseDirName)

    Iterator.iterate(baseDir)(_.getParentFile).takeWhile(_ != null).toList.reverse.filterNot(_.getName.isEmpty).foldLeft("/") {
      (path, file) ⇒
        val childPath = child(path, file.getName)
        if (!exists(childPath)) makeDir(childPath)
        childPath
    }

  }

  override def toString: String = id
  def withToken[A](a: (AccessToken) ⇒ A): A =
    UsageControl.withToken(id)(a)

  private def withFailureControl[A](a: ⇒ A): A = QualityControl.withFailureControl(StorageControl.qualityControl(id))(a)

  def exists(path: String)(implicit token: AccessToken): Boolean = withFailureControl { super.exists(path) }
  def listNames(path: String)(implicit token: AccessToken): Seq[String] = withFailureControl { super.listNames(path) }
  def list(path: String)(implicit token: AccessToken): Seq[(String, FileType)] = withFailureControl { super.list(path) }
  def makeDir(path: String)(implicit token: AccessToken): Unit = withFailureControl { super.makeDir(path) }
  def rmDir(path: String)(implicit token: AccessToken): Unit = withFailureControl { super.rmDir(path) }
  def rmFile(path: String)(implicit token: AccessToken): Unit = withFailureControl { super.rmFile(path) }
  def openInputStream(path: String)(implicit token: AccessToken): InputStream = withFailureControl { super.openInputStream(path) }
  def openOutputStream(path: String)(implicit token: AccessToken): OutputStream = withFailureControl { super.openOutputStream(path) }

  def upload(src: File, dest: String)(implicit token: AccessToken) = withFailureControl { super.upload(src, dest) }
  def uploadGZ(src: File, dest: String)(implicit token: AccessToken) = withFailureControl { super.uploadGZ(src, dest) }
  def download(src: String, dest: File)(implicit token: AccessToken) = withFailureControl { super.download(src, dest) }
  def downloadGZ(src: String, dest: File)(implicit token: AccessToken) = withFailureControl { super.downloadGZ(src, dest) }

  def baseDirName = Workspace.preference(Workspace.uniqueID) + '/'

  def backgroundRmFile(path: String) = environment.jobManager ! DeleteFile(this, path, false)
  def backgroundRmDir(path: String) = environment.jobManager ! DeleteFile(this, path, true)

}
