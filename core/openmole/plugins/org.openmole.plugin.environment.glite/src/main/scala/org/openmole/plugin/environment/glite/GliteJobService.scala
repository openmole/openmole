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
import org.openmole.core.batch.storage.Storage
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

object GliteJobService extends Logger

import GliteJobService._

trait GliteJobService extends GridScaleJobService with JobServiceQualityControl with LimitedAccess { js ⇒

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

      val os = script.bufferedOutputStream
      try generateScript(serializedJob, outputFilePath, os)
      finally os.close

      val jobDescription = buildJobDescription(runtime, script)

      val jid = jobService.submit(jobDescription)(authentication)

      new GliteJob {
        val jobService = js
        val id = jid
        val resultPath = outputFilePath
      }
    } finally script.delete
  }

  protected def generateScript(serializedJob: SerializedJob, resultPath: String, os: OutputStream) = {
    import serializedJob._

    val writter = new PrintStream(os)

    assert(runtime.runtime.path != null)
    writter.print("BASEPATH=$PWD;CUR=$PWD/ws$RANDOM;while test -e $CUR; do CUR=$PWD/ws$RANDOM;done;mkdir $CUR; export HOME=$CUR; cd $CUR; export OPENMOLE_HOME=$CUR; ")
    writter.print("if [ `uname -m` = x86_64 ]; then ")
    writter.print(lcgCpGunZipCmd(environment, storage.url.resolve(runtime.jvmLinuxX64.path), "$PWD/jvm.tar.gz"))
    writter.print("else ")
    writter.print(lcgCpGunZipCmd(environment, storage.url.resolve(runtime.jvmLinuxI386.path), "$PWD/jvm.tar.gz"))
    writter.print("fi; ")
    writter.print("tar -xzf jvm.tar.gz >/dev/null; rm -f jvm.tar.gz; ")
    writter.print(lcgCpGunZipCmd(environment, storage.url.resolve(runtime.runtime.path), "$PWD/openmole.tar.gz"))
    writter.print("tar -xzf openmole.tar.gz >/dev/null; rm -f openmole.tar.gz; ")
    writter.print("mkdir envplugins; PLUGIN=0;")

    for (plugin ← runtime.environmentPlugins) {
      assert(plugin.path != null)
      writter.print(lcgCpGunZipCmd(environment, storage.url.resolve(plugin.path), "$CUR/envplugins/plugin$PLUGIN.jar"))
      writter.print("PLUGIN=`expr $PLUGIN + 1`; ")
    }

    writter.print(lcpCpCmd(environment, storage.url.resolve(runtime.storage.path), "$CUR/storage.xml.gz"))

    writter.print(" export PATH=$PWD/jre/bin:$PATH; /bin/sh run.sh ")
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
    //    writter.print(" -w $CUR") // 2>err.txt ; lcg-cp file:$PWD/err.txt " + path.toStringURI("err.txt")) 
    writter.print("; cd .. ; rm -rf $CUR")
  }

  private def lcpCpCmd(env: GliteEnvironment, from: URI, to: String) = {
    val builder = new StringBuilder

    builder.append("lcg-cp --vo ")
    builder.append(env.voName)
    builder.append(" --checksum --connect-timeout ")
    builder.append(getTimeOut)
    builder.append(" --sendreceive-timeout ")
    builder.append(getTimeOut)
    builder.append(" --bdii-timeout ")
    builder.append(getTimeOut)
    builder.append(" --srm-timeout ")
    builder.append(getTimeOut)
    builder.append(" ")
    builder.append(from.toString)
    builder.append(" file:")
    builder.append(to)
    builder.append("; ")
    builder.toString
  }

  private def lcgCpGunZipCmd(env: GliteEnvironment, from: URI, to: String) = {
    val builder = new StringBuilder
    builder.append(lcpCpCmd(env, from, to + ".gz"))
    builder.append("gunzip ")
    builder.append(to)
    builder.append(".gz; ")

    builder.toString
  }

  private def getTimeOut = Workspace.preferenceAsDurationInS(GliteEnvironment.RemoteTimeout).toString

  protected def buildJobDescription(runtime: Runtime, script: File) =
    new WMSJobDescription {
      val executable = "/bin/bash"
      val arguments = script.getName
      val inputSandbox = List(script.getAbsolutePath)
      val outputSandbox = List.empty
      override val memory = Some(environment.requieredMemory)
      override val cpuTime = environment.cpuTime.map(_.toMinutes)
      override val cpuNumber = environment.cpuNumber
      override val jobType = environment.jobType
      override val smpGranularity = environment.smpGranularity
      override val retryCount = Some(Workspace.preferenceAsInt(GliteEnvironment.WMSRetryCount))
      override val myProxyServer = environment.myProxy.map(_.url)
      override val architecture = environment.architecture
      //override val fuzzy = true
    }

  //  
  //    val description = newJobDescription
  //
  //    description.setAttribute(JobDescription.EXECUTABLE, "/bin/bash")
  //    description.setVectorAttribute(JobDescription.ARGUMENTS, Array[String](script.getName))
  //
  //    /*description.setAttribute(JobDescription.OUTPUT, "out.txt")
  //    description.setAttribute(JobDescription.ERROR, "err.txt")*/
  //
  //    description.setVectorAttribute(JobDescription.FILETRANSFER, Array[String]("file:/" +
  //      { if (script.getAbsolutePath.startsWith("/")) script.getAbsolutePath.tail else script.getAbsolutePath } +
  //      ">" + script.getName))
  //
  //    environment.allRequirements.get(GLITE_REQUIREMENTS) match {
  //      case Some(requirement) ⇒
  //        val requirements = new StringBuilder
  //        requirements.append("JDLRequirements=(")
  //        requirements.append(requirement)
  //        requirements.append(')')
  //
  //        description.setVectorAttribute("Extension", Array[String](requirements.toString))
  //      case None ⇒
  //    }
  //
  //    environment.authentication.myProxy match {
  //      case Some(myProxy) ⇒
  //        description.setAttribute(VOMSContext.MYPROXYSERVER, myProxy.url)
  //      case None ⇒
  //    }
  //
  //    description
  //  }
}
