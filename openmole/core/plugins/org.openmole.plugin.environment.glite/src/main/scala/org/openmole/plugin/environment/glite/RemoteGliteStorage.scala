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

import org.openmole.core.batch.storage.{ RemoteStorage, SimpleStorage }
import org.openmole.misc.workspace._
import org.openmole.misc.exception._
import org.openmole.misc.tools.io.FileUtil._
import fr.iscpif.gridscale.{ Storage ⇒ GSStorage }
import java.io.File
import java.net.URI
import scala.sys.process._

class RemoteGliteStorage(val host: String, val port: Int, voName: String) extends RemoteStorage { s ⇒

  val timeout = Workspace.preferenceAsDuration(GliteEnvironment.RemoteTimeout).toSeconds

  @transient lazy val url = new URI("srm", null, host, port, null, null, null)

  def lcgCp = s"lcg-cp --vo ${voName} --checksum $timeOutCmd "
  def lcgDel = s"lcg-del --vo ${voName} $timeOutCmd "

  def timeOutCmd = s"--connect-timeout $timeout --sendreceive-timeout $timeout --srm-timeout $timeout"

  def lcgCpCmd(from: String, to: URI) = s"$lcgCp file:$from ${to.toString}"
  def lcgCpCmd(from: URI, to: String) = s"$lcgCp ${from.toString} file:$to"

  protected def run(cmd: String) = {
    val output = new StringBuilder
    val error = new StringBuilder

    val logger =
      ProcessLogger(
        (o: String) ⇒ output.append(o),
        (e: String) ⇒ error.append(e)
      )

    val exit = Process(cmd) ! logger
    if (exit != 0) throw new RuntimeException(s"Command $cmd had a non 0 return value. Output: ${output.toString}. Error: ${error.toString}")
    output.toString
  }

  override def child(parent: String, child: String): String = GSStorage.child(parent, child)

  override def downloadGZ(src: String, dest: File): Unit = Workspace.withTmpFile { tmpFile ⇒
    download(src, tmpFile)
    tmpFile.copyUncompressFile(dest)
  }

  override def download(src: String, dest: File): Unit = run(lcgCpCmd(url.resolve(src), dest.getAbsolutePath))

  override def uploadGZ(src: File, dest: String): Unit = Workspace.withTmpFile { tmpFile ⇒
    src.copyCompress(tmpFile)
    uploadGZ(tmpFile, dest)
  }

  override def upload(src: File, dest: String): Unit = run(lcgCpCmd(src.getAbsolutePath, url.resolve(dest)))
}
