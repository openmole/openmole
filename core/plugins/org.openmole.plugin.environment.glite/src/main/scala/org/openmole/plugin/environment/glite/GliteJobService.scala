/*
 * Copyright (C) 2010 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.glite

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URI
import java.util.UUID
import org.openmole.core.batch.control._
import org.openmole.core.batch.storage.{ StorageService, Storage }
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.batch.environment.Runtime
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.core.batch.control.AccessToken
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.plugin.environment.gridscale.GridScaleJobService
import fr.iscpif.gridscale.jobservice.{ WMSJobService, WMSJobDescription }
import scala.collection.JavaConversions._
import scala.io.Source
import org.openmole.misc.tools.service.Duration._

object GliteJobService extends Logger {
  val finishedFile = "finished"
  val runningFile = "running"
}

import GliteJobService._

trait GliteJobService extends GridScaleJobService with JobServiceQualityControl with LimitedAccess with AvailabitityQuality { js ⇒

  val jobService: WMSJobService
  def environment: GliteEnvironment

  def authentication = environment.authentication._1
  def proxyFile = environment.authentication._2

  lazy val id = jobService.url.toString
  def hysteresis = Workspace.preferenceAsInt(GliteEnvironment.QualityHysteresis)

  var delegated = false

  def delegateProxy = jobService.delegateProxy(proxyFile)(authentication)

  def checkDelegated = synchronized {
    if (!delegated) {
      delegateProxy
      delegated = true
    }
  }

  override protected def _purge(j: J) = quality {
    checkDelegated
    super._purge(j)
  }

  override protected def _cancel(j: J) = quality {
    checkDelegated
    super._cancel(j)
  }

  override protected def _state(j: J) = quality {
    checkDelegated
    super._state(j)
  }

  override protected def _submit(serializedJob: SerializedJob) = quality {
    checkDelegated
    import serializedJob._

    val script = Workspace.newFile("script", ".sh")
    try {
      val outputFilePath = storage.child(path, Storage.uniqName("job", ".out"))
      val _runningPath = serializedJob.storage.child(path, runningFile)
      val _finishedPath = serializedJob.storage.child(path, finishedFile)

      val os = script.bufferedOutputStream
      try generateScript(serializedJob, outputFilePath, _runningPath, _finishedPath, os)
      finally os.close

      //logger.fine(ISource.fromFile(script).mkString)

      val jobDescription = buildJobDescription(runtime, script)

      //logger.fine(jobDescription.toJDL)

      val jid = jobService.submit(jobDescription)(authentication)

      new GliteJob {
        val jobService = js
        val storage = serializedJob.storage
        val finishedPath = _finishedPath
        val runningPath = _runningPath
        val id = jid
        val resultPath = outputFilePath
      }
    }
    finally script.delete
  }

  protected def generateScript(
    serializedJob: SerializedJob,
    resultPath: String,
    runningPath: String,
    finishedPath: String,
    os: OutputStream) = {
    import serializedJob._

    val writter = new PrintStream(os)

    assert(runtime.runtime.path != null)

    writter.print(touch(storage.url.resolve(runningPath)))
    writter.print("; BASEPATH=$PWD; CUR=$PWD/ws$RANDOM; while test -e $CUR; do CUR=$PWD/ws$RANDOM;done;mkdir $CUR; export HOME=$CUR; cd $CUR; export OPENMOLE_HOME=$CUR; ")
    writter.print("if [ `uname -m` = x86_64 ]; then ")
    writter.print(lcgCpGunZipCmd(storage.url.resolve(runtime.jvmLinuxX64.path), "$PWD/jvm.tar.gz")) //, homeCacheDir, runtime.jvmLinuxX64.hash))
    writter.print("; else ")
    writter.print(lcgCpGunZipCmd(storage.url.resolve(runtime.jvmLinuxI386.path), "$PWD/jvm.tar.gz")) //, homeCacheDir, runtime.jvmLinuxI386.hash))
    writter.print("; fi; ")
    writter.print("tar -xzf jvm.tar.gz >/dev/null; rm -f jvm.tar.gz; ")
    writter.print(lcgCpGunZipCmd(storage.url.resolve(runtime.runtime.path), "$PWD/openmole.tar.gz")) //, homeCacheDir, runtime.runtime.hash))
    writter.print("; tar -xzf openmole.tar.gz >/dev/null; rm -f openmole.tar.gz; ")
    writter.print("mkdir envplugins; PLUGIN=0;")

    for (plugin ← runtime.environmentPlugins) {
      assert(plugin.path != null)
      writter.print(lcgCpGunZipCmd(storage.url.resolve(plugin.path), "$CUR/envplugins/plugin$PLUGIN.jar")) //, homeCacheDir, plugin.hash))
      writter.print("; PLUGIN=`expr $PLUGIN + 1`; ")
    }

    writter.print(lcgCpCmd(storage.url.resolve(runtime.storage.path), "$CUR/storage.xml.gz"))

    writter.print(" ; export PATH=$PWD/jre/bin:$PATH; /bin/sh run.sh ")
    writter.print(environment.openMOLEMemoryValue)
    writter.print("m ")
    writter.print(UUID.randomUUID)
    writter.print(" -c ")
    writter.print(path)
    writter.print(" -s $CUR/storage.xml.gz ")
    writter.print(" -p $CUR/envplugins/ ")
    writter.print(" -i ")
    writter.print(inputFile)
    writter.print(" -o ")
    writter.print(resultPath)
    writter.print(" -t ")
    writter.print(environment.threadsValue)
    writter.print("; ")
    writter.print(touch(storage.url.resolve(finishedPath)))
    writter.print("; cd .. ; rm -rf $CUR ; ")
  }

  protected def touch(dest: URI) = {
    val name = UUID.randomUUID.toString
    s"touch $name; ${lcgCpCmd(name, dest)}; rm $name "
  }

  protected def lcgCpGunZipCmd(from: URI, to: String) = {
    val builder = new StringBuilder
    builder.append(lcgCpCmd(from, to + ".gz"))
    builder.append(" && gunzip ")
    builder.append(to)
    builder.append(".gz ")
    builder.toString
  }

  @transient lazy val lcgCp =
    s"lcg-cp --vo ${environment.voName} --checksum --connect-timeout $getTimeOut --sendreceive-timeout $getTimeOut --bdii-timeout $getTimeOut --srm-timeout $getTimeOut "

  protected def lcgCpCmd(from: String, to: URI) = s"$lcgCp file:$from ${to.toString}"
  protected def lcgCpCmd(from: URI, to: String) = s"$lcgCp ${from.toString} file:$to"

  private def getTimeOut = Workspace.preferenceAsDuration(GliteEnvironment.RemoteTimeout).toSeconds.toString

  protected def buildJobDescription(runtime: Runtime, script: File) =
    new WMSJobDescription {
      val executable = "/bin/bash"
      val arguments = script.getName
      val inputSandbox = List(script.getAbsolutePath)
      val outputSandbox = List.empty
      override val memory = Some(environment.requieredMemory)
      override val cpuTime = environment.cpuTime.map(_.toMinutes)
      override val wallTime = environment.wallTime.map(_.toMinutes)
      override val cpuNumber = environment.cpuNumber orElse environment.threads
      override val jobType = environment.jobType
      override val smpGranularity = environment.smpGranularity orElse environment.threads
      override val retryCount = Some(0)
      override val shallowRetryCount = Some(Workspace.preferenceAsInt(GliteEnvironment.ShallowWMSRetryCount))
      override val myProxyServer = environment.myProxy.map(_.url)
      override val architecture = environment.architecture
      override val fuzzy = true
    }

}
