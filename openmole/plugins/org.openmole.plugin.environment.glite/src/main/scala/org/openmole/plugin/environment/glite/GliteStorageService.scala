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

package org.openmole.plugin.environment.glite

import org.openmole.core.batch.storage._
import org.openmole.core.batch.control._
import org.openmole.misc.workspace._
import fr.iscpif.gridscale.storage.FileType
import fr.iscpif.gridscale.storage.SRMStorage
import java.net.URI
import java.io.{ File, InputStream, OutputStream }

object GliteStorageService {

  def emptyRoot(s: SRMStorage) =
    new SRMStorage {
      val host: String = s.host
      val port: Int = s.port
      val basePath: String = ""
    }

  def apply(s: SRMStorage, _environment: GliteEnvironment, caCertDir: File) = new GliteStorageService {
    val storage = emptyRoot(s)
    val url = new URI("srm", null, s.host, s.port, null, null, null)
    val remoteStorage = new RemoteGliteStorage(s.host, s.port, caCertDir)
    val environment = _environment
    val root = s.basePath
    val connections = _environment.threadsBySE
    def authentication = environment.authentication._1
  }

}

trait GliteStorageService extends PersistentStorageService {
  @transient lazy val qualityControl = new QualityControl(Workspace.preferenceAsInt(GliteEnvironment.QualityHysteresis))
  def withQualityControl[A](op: ⇒ A) = QualityControl.withFailureControl(qualityControl)(op)

  override def exists(path: String)(implicit token: AccessToken): Boolean = withQualityControl { super.exists(path)(token) }
  override def listNames(path: String)(implicit token: AccessToken): Seq[String] = withQualityControl { super.listNames(path)(token) }
  override def list(path: String)(implicit token: AccessToken): Seq[(String, FileType)] = withQualityControl { super.list(path)(token) }
  override def makeDir(path: String)(implicit token: AccessToken): Unit = withQualityControl { super.makeDir(path)(token) }
  override def rmDir(path: String)(implicit token: AccessToken): Unit = withQualityControl { super.rmDir(path)(token) }
  override def rmFile(path: String)(implicit token: AccessToken): Unit = withQualityControl { super.rmFile(path)(token) }
  override def openInputStream(path: String)(implicit token: AccessToken): InputStream = withQualityControl { super.openInputStream(path)(token) }
  override def openOutputStream(path: String)(implicit token: AccessToken): OutputStream = withQualityControl { super.openOutputStream(path)(token) }

  override def upload(src: File, dest: String)(implicit token: AccessToken) = withQualityControl { super.upload(src, dest)(token) }
  override def uploadGZ(src: File, dest: String)(implicit token: AccessToken) = withQualityControl { super.uploadGZ(src, dest)(token) }
  override def download(src: String, dest: File)(implicit token: AccessToken) = withQualityControl { super.download(src, dest)(token) }
  override def downloadGZ(src: String, dest: File)(implicit token: AccessToken) = withQualityControl { super.downloadGZ(src, dest)(token) }
}
