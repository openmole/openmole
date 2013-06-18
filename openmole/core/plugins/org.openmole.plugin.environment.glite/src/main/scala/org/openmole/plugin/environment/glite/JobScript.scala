/*
 * Copyright (C) 10/06/13 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.glite

import org.openmole.core.batch.environment.{ BatchEnvironment, SerializedJob }
import java.util.UUID
import java.net.URI
import org.openmole.misc.workspace.Workspace
import scala.collection.mutable.ListBuffer

trait JobScript {

  def environment: BatchEnvironment {
    def voName: String
  }

  protected def generateScript(
    serializedJob: SerializedJob,
    resultPath: String,
    runningPath: Option[String],
    finishedPath: Option[String]) = {
    import serializedJob._

    assert(runtime.runtime.path != null)

    val init = {
      val script = ListBuffer[String]()

      script += "BASEPATH=$PWD"
      script += "CUR=$PWD/ws$RANDOM"
      script += "while test -e $CUR; do export CUR=$PWD/ws$RANDOM; done"
      script += "mkdir $CUR"
      script += "export HOME=$CUR"
      script += "cd $CUR"
      script += "export OPENMOLE_HOME=$CUR"

      runningPath.map(p ⇒ touch(storage.url.resolve(p)) + "; ").getOrElse("") + script.mkString(" && ")
    }

    val install = {
      val script = ListBuffer[String]()

      script +=
        "if [ `uname -m` = x86_64 ]; then " +
        lcgCpGunZipCmd(storage.url.resolve(runtime.jvmLinuxX64.path), "$PWD/jvm.tar.gz") + "; else " +
        lcgCpGunZipCmd(storage.url.resolve(runtime.jvmLinuxI386.path), "$PWD/jvm.tar.gz") + "; fi"
      script += "tar -xzf jvm.tar.gz >/dev/null"
      script += "rm -f jvm.tar.gz"
      script += lcgCpGunZipCmd(storage.url.resolve(runtime.runtime.path), "$PWD/openmole.tar.gz")
      script += "tar -xzf openmole.tar.gz >/dev/null"
      script += "rm -f openmole.tar.gz"
      script.mkString(" && ")
    }

    val dl = {
      val script = ListBuffer[String]()

      for { (plugin, index) ← runtime.environmentPlugins.zipWithIndex } {
        assert(plugin.path != null)
        script += lcgCpGunZipCmd(storage.url.resolve(plugin.path), "$CUR/envplugins/plugin" + index + ".jar")
      }

      script += lcgCpCmd(storage.url.resolve(runtime.storage.path), "$CUR/storage.xml.gz")

      "mkdir envplugins && " + script.mkString(" && ")
    }

    val run = {
      val script = ListBuffer[String]()

      script += "export PATH=$PWD/jre/bin:$PATH"
      script += "/bin/sh run.sh " + environment.openMOLEMemoryValue + "m " + UUID.randomUUID + " -c " +
        path + " -s $CUR/storage.xml.gz -p $CUR/envplugins/ -i " + inputFile + " -o " + resultPath +
        " -t " + environment.threadsValue
      script.mkString(" && ")
    }

    val finish =
      finishedPath.map { p ⇒ touch(storage.url.resolve(p)) + "; " }.getOrElse("") + "cd .. &&  rm -rf $CUR"

    init + " && " + install + " && " + dl + " && " + run + "; RETURNCODE=$?;" + finish + "; exit $RETURNCODE;"
  }

  protected def touch(dest: URI) = {
    val name = UUID.randomUUID.toString
    s"touch $name && ${lcgCpCmd(name, dest)}; rm -f $name"
  }

  protected def lcgCpGunZipCmd(from: URI, to: String) =
    s"( ${lcgCpCmd(from, to + ".gz")} && gunzip $to.gz )"

  @transient lazy val lcgCp =
    s"lcg-cp --vo ${environment.voName} --checksum --connect-timeout $getTimeOut --sendreceive-timeout $getTimeOut --srm-timeout $getTimeOut "

  protected def lcgCpCmd(from: String, to: URI) = s"$lcgCp file:$from ${to.toString}"

  protected def lcgCpCmd(from: URI, to: String) = s"$lcgCp ${from.toString} file:$to"

  private def getTimeOut = Workspace.preferenceAsDuration(GliteEnvironment.RemoteTimeout).toSeconds.toString

  private def background(s: String) = "( " + s + " & )"
}
