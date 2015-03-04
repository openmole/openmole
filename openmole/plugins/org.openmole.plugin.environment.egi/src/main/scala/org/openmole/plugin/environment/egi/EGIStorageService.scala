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
import fr.iscpif.gridscale.storage.FileType
import fr.iscpif.gridscale.glite.{ SRMStorage, GlobusAuthentication }
import java.net.URI
import java.io.{ File, InputStream, OutputStream }
import org.openmole.core.batch.environment.BatchEnvironment

object EGIStorageService {

  def emptyRoot(s: SRMStorage, threads: Int) =
    new SRMStorage {
      val host: String = s.host
      val port: Int = s.port
      val basePath: String = ""
      val credential = s.credential
      override val connections = threads
    }

  def apply(s: SRMStorage, _environment: BatchEnvironment { def voName: String }, _authentication: GlobusAuthentication.ProxyCreator, threads: Int) = new EGIStorageService {
    val storage = emptyRoot(s, threads)
    val url = new URI("srm", null, s.host, s.port, null, null, null)
    val remoteStorage = new RemoteGliteStorage(s.host, s.port, _environment.voName)
    val environment = _environment
    val root = s.basePath
    override lazy val id = new URI("srm", environment.voName, s.host, s.port, s.basePath, null, null).toString
    def nbTokens = threads
    def authentication = _authentication
  }

}

trait EGIStorageService extends PersistentStorageService with QualityControl with LimitedAccess with AvailabitityQuality {
  def hysteresis = Workspace.preferenceAsInt(EGIEnvironment.QualityHysteresis)

  override def exists(path: String)(implicit token: AccessToken): Boolean = quality { super.exists(path)(token) }
  override def listNames(path: String)(implicit token: AccessToken): Seq[String] = quality { super.listNames(path)(token) }
  override def list(path: String)(implicit token: AccessToken): Seq[(String, FileType)] = quality { super.list(path)(token) }
  override def makeDir(path: String)(implicit token: AccessToken): Unit = quality { super.makeDir(path)(token) }
  override def rmDir(path: String)(implicit token: AccessToken): Unit = quality { super.rmDir(path)(token) }
  override def rmFile(path: String)(implicit token: AccessToken): Unit = quality { super.rmFile(path)(token) }
  override def openInputStream(path: String)(implicit token: AccessToken): InputStream = quality { super.openInputStream(path)(token) }
  override def openOutputStream(path: String)(implicit token: AccessToken): OutputStream = quality { super.openOutputStream(path)(token) }

  override def upload(src: File, dest: String)(implicit token: AccessToken) = quality { super.upload(src, dest)(token) }
  override def uploadGZ(src: File, dest: String)(implicit token: AccessToken) = quality { super.uploadGZ(src, dest)(token) }
  override def download(src: String, dest: File)(implicit token: AccessToken) = quality { super.download(src, dest)(token) }
  override def downloadGZ(src: String, dest: File)(implicit token: AccessToken) = quality { super.downloadGZ(src, dest)(token) }

}
