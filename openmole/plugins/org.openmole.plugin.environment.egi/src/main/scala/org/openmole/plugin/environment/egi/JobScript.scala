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

import java.util.UUID

import org.openmole.core.preference.Preference
import org.openmole.plugin.environment.batch.environment.SerializedJob

import scala.collection.mutable.ListBuffer

object JobScript:

  def create(
    serializedJob:   SerializedJob,
    resultPath:      String,
    storageLocation: String,
    voName:          String,
    memory:          Int,
    debug:           Boolean,
    proxy:           Option[String] = None
  )(using preference: Preference) =
    import serializedJob._

    def cpCommand = CurlRemoteStorage.Curl(voName, debug, preference(EGIEnvironment.RemoteCopyTimeout))

    assert(runtime.runtime.path != null)

    def resolve(dest: String) = gridscale.RemotePath.child(storageLocation, dest)

    val retry = 5
    val functions =
      """
        |retry() {
        |    local -r -i max_attempts="$1"; shift
        |    local -i attempt_num=1
        |    until "$@"
        |    do
        |        if ((attempt_num==max_attempts))
        |        then
        |            echo "Attempt $attempt_num failed and there are no more attempts left!"
        |            return 1
        |        else
        |            echo "Attempt $attempt_num failed! Trying again in $attempt_num seconds..."
        |            sleep $((attempt_num++))
        |        fi
        |    done
        |}
        |
        |""".stripMargin

    val debugInfo = s"echo $storageLocation ; hostname ; date -R ; cat /proc/meminfo ; ulimit -n 10240 ; ulimit -a ; " + "env ; echo $X509_USER_PROXY ; "

    val init =
      val script = ListBuffer[String]()

      proxy.foreach { p => script += s"export X509_USER_PROXY=$$PWD/$p" }

      script += "ARGS=()"
      script += """while [[ $# -gt 0 ]]; do
                  |  case "$1" in
                  |    --unique-id ) UNIQUE_ID=$2 ;;
                  |    * ) ARGS+=("$1");;
                  |  esac
                  |  shift
                  |done""".stripMargin

      script += """echo Job running for unique id: $UNIQUE_ID"""
      script += "unset http_proxy"
      script += "unset https_proxy"
      script += "BASEPATH=$PWD"
      script += "CUR=$PWD/ws$RANDOM"
      script += "while test -e $CUR; do export CUR=$PWD/ws$RANDOM; done"
      script += "mkdir $CUR"
      script += "export HOME=$CUR"
      script += "cd $CUR"

      script.mkString(" && ")

    val install =
      val script = ListBuffer[String]()

      script +=
        "if [ `uname -m` = x86_64 ]; then " +
        s"retry $retry " + cpCommand.download(resolve(runtime.jvmLinuxX64.path), "$PWD/jvm.tar.gz") + "; else " +
        """echo "Unsupported architecture: " `uname -m`; exit 1; fi"""
      script += "tar -xzf jvm.tar.gz >/dev/null"
      script += "rm -f jvm.tar.gz"
      script += s"retry $retry " + cpCommand.download(resolve(runtime.runtime.path), "$PWD/openmole.tar.gz")
      script += "tar -xzf openmole.tar.gz >/dev/null"
      script += "rm -f openmole.tar.gz"
      script.mkString(" && ")

    val dl =
      val script = ListBuffer[String]()

      for (plugin, index) ← runtime.environmentPlugins.zipWithIndex
      do
        assert(plugin.path != null)
        script += s"retry $retry " + cpCommand.download(resolve(plugin.path), "$CUR/envplugins/plugin" + index + ".jar")

      script += s"retry $retry " + cpCommand.download(resolve(serializedJob.remoteStorage.path), "$CUR/storage.bin")

      "mkdir -p envplugins && " + script.mkString(" && ")

    val run =
      val script = ListBuffer[String]()

      script += "export PATH=$PWD/jre/bin:$PATH"
      script += "export HOME=$PWD"
      script += s"""/bin/sh run.sh ${memory}m ${UUID.randomUUID} -s $$CUR/storage.bin -p $$CUR/envplugins/ -i $inputPath -o $resultPath --transfer-retry $retry""" + (if debug then " -d 2>&1" else "")
      script.mkString(" && ")

    val postDebugInfo = if (debug) "cat *.log ; " else ""

    val finish = "cd .. &&  rm -rf $CUR"

    functions + debugInfo + init + " && " + install + " && " + dl + " && " + run + s"; RETURNCODE=${if debug then "0" else "$?"};" + postDebugInfo + finish + "; exit $RETURNCODE;"

  private def background(s: String) = "( " + s + " & )"

