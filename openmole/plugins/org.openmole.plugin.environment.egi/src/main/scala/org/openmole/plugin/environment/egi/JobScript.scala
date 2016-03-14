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

package org.openmole.plugin.environment.egi

import org.openmole.core.batch.environment.{ BatchEnvironment, SerializedJob }
import java.util.UUID
import java.net.URI
import org.openmole.core.workspace.Workspace

import scala.collection.mutable.ListBuffer

case class JobScript(voName: String, memory: Int, threads: Int, debug: Boolean) {

  def apply(
    serializedJob: SerializedJob,
    resultPath: String,
    runningPath: Option[String] = None,
    finishedPath: Option[String] = None,
    proxy: Option[String] = None) = {
    import serializedJob._

    def cpCommand =
      serializedJob.storage match {
        case _: EGIWebDAVStorageService ⇒ Curl(voName, debug)
        case _                          ⇒ LCGCp(voName)
      }

    assert(runtime.runtime.path != null)

    val debugInfo =
      if (debug) s"echo ${serializedJob.storage.url} ; cat /proc/meminfo ; ulimit -a ; " + "env ; echo $X509_USER_PROXY ; cat $X509_USER_PROXY ; "
      else ""

    val init = {
      val script = ListBuffer[String]()

      proxy.foreach { p ⇒ script += s"export X509_USER_PROXY=$$PWD/$p" }

      if (debug) script += "voms-proxy-info -all"

      script += "BASEPATH=$PWD"
      script += "CUR=$PWD/ws$RANDOM"
      script += "while test -e $CUR; do export CUR=$PWD/ws$RANDOM; done"
      script += "mkdir $CUR"
      script += "export HOME=$CUR"
      script += "cd $CUR"
      script += "export OPENMOLE_HOME=$CUR"

      runningPath.map(p ⇒ touch(storage.url.resolve(p), cpCommand) + "; ").getOrElse("") + script.mkString(" && ")
    }

    val install = {
      val script = ListBuffer[String]()

      script +=
        "if [ `uname -m` = x86_64 ]; then " +
        cpCommand.download(storage.url.resolve(runtime.jvmLinuxX64.path), "$PWD/jvm.tar.gz") + "; else " +
        cpCommand.download(storage.url.resolve(runtime.jvmLinuxI386.path), "$PWD/jvm.tar.gz") + "; fi"
      script += "tar -xzf jvm.tar.gz >/dev/null"
      script += "rm -f jvm.tar.gz"
      script += cpCommand.download(storage.url.resolve(runtime.runtime.path), "$PWD/openmole.tar.gz")
      script += "tar -xzf openmole.tar.gz >/dev/null"
      script += "rm -f openmole.tar.gz"
      script.mkString(" && ")
    }

    val dl = {
      val script = ListBuffer[String]()

      for { (plugin, index) ← runtime.environmentPlugins.zipWithIndex } {
        assert(plugin.path != null)
        script += cpCommand.download(storage.url.resolve(plugin.path), "$CUR/envplugins/plugin" + index + ".jar")
      }

      script += cpCommand.download(storage.url.resolve(runtime.storage.path), "$CUR/storage.xml")

      "mkdir envplugins && " + script.mkString(" && ")
    }

    val run = {
      val script = ListBuffer[String]()

      script += "export PATH=$PWD/jre/bin:$PATH"
      script += "/bin/sh run.sh " + memory + "m " + UUID.randomUUID + " -c " +
        path + " -s $CUR/storage.xml -p $CUR/envplugins/ -i " + inputFile + " -o " + resultPath +
        " -t " + threads + (if (debug) " -d 2>&1" else "")
      script.mkString(" && ")
    }

    val postDebugInfo = if (debug) "cat *.log ; " else ""

    val finish =
      finishedPath.map { p ⇒ touch(storage.url.resolve(p), cpCommand) + "; " }.getOrElse("") + "cd .. &&  rm -rf $CUR"

    debugInfo + init + " && " + install + " && " + dl + " && " + run + s"; RETURNCODE=${if (debug) "0" else "$?"};" + postDebugInfo + finish + "; exit $RETURNCODE;"
  }

  protected def touch(dest: URI, cpCommand: CpCommands) = {
    val name = UUID.randomUUID.toString
    s"echo $name >$name && ${cpCommand.upload(name, dest)}; rm -f $name"
  }

  private def background(s: String) = "( " + s + " & )"
}
