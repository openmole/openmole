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

import fr.in2p3.jsaga.adaptor.security.VOMSContext
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URI
import java.util.UUID
import org.ogf.saga.job.Job
import org.ogf.saga.job.JobDescription
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.misc.exception.InternalProcessingError
import org.ogf.saga.job.JobFactory
import org.openmole.core.batch.environment.Runtime
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga.JSAGAJob
import org.openmole.plugin.environment.jsaga.JSAGAJobService
import org.openmole.core.batch.control.AccessToken
import org.openmole.misc.tools.io.FileUtil._
import scala.collection.JavaConversions._
import org.ogf.saga.error._
import scala.io.Source

object GliteJobService extends Logger

class GliteJobService(
    val uri: URI,
    val environment: GliteEnvironment,
    override val connections: Int) extends JSAGAJobService {

  import GliteJobService._

  override protected def doSubmit(serializedJob: SerializedJob, token: AccessToken) = {
    import serializedJob._
    import communicationStorage.path

    val script = Workspace.newFile("script", ".sh")
    try {
      val outputFilePath = path.toURIFile(communicationDirPath).newFileInDir("job", ".out").path

      val os = script.bufferedOutputStream
      try generateScript(serializedJob, outputFilePath, environment.runtimeMemory.intValue, os)
      finally os.close

      val jobDescription = buildJobDescription(runtime, script)
      val job = jobService.createJob(jobDescription)
      try job.run
      catch {
        case t: TimeoutException ⇒ job.cancel(true)
      }
      //logger.fine(Source.fromFile(script).getLines.mkString)

      new GliteJob(JSAGAJob.id(job), outputFilePath, this, environment.authentication.expires)
    } finally script.delete
  }

  protected def generateScript(serializedJob: SerializedJob, resultPath: String, memorySizeForRuntime: Int, os: OutputStream) = {
    import serializedJob.communicationStorage.path
    import serializedJob._

    val writter = new PrintStream(os)

    assert(runtime.runtime.path != null)
    writter.print("BASEPATH=$PWD;CUR=$PWD/ws$RANDOM;while test -e $CUR; do CUR=$PWD/ws$RANDOM;done;mkdir $CUR; export HOME=$CUR; cd $CUR; export OPENMOLE_HOME=$CUR; ")
    writter.print("if [ `uname -m` = x86_64 ]; then ")
    writter.print(mkLcgCpGunZipCmd(environment, path.toStringURI(runtime.jvmLinuxX64.path), "$PWD/jvm.tar.gz"))
    writter.print("else ")
    writter.print(mkLcgCpGunZipCmd(environment, path.toStringURI(runtime.jvmLinuxI386.path), "$PWD/jvm.tar.gz"))
    writter.print("fi; ")
    writter.print("tar -xzf jvm.tar.gz >/dev/null; rm -f jvm.tar.gz; ")
    writter.print(mkLcgCpGunZipCmd(environment, path.toStringURI(runtime.runtime.path), "$PWD/openmole.tar.gz"))
    writter.print("tar -xzf openmole.tar.gz >/dev/null; rm -f openmole.tar.gz; ")
    writter.print("mkdir envplugins; PLUGIN=0;")

    for (plugin ← runtime.environmentPlugins) {
      assert(plugin.path != null)
      writter.print(mkLcgCpGunZipCmd(environment, path.toStringURI(plugin.path), "$CUR/envplugins/plugin$PLUGIN.jar"))
      writter.print("PLUGIN=`expr $PLUGIN + 1`; ")
    }

    assert(runtime.authentication.path != null)
    writter.print(mkLcgCpGunZipCmd(environment, path.toStringURI(runtime.authentication.path), "$CUR/authentication.xml"))

    writter.print(" export PATH=$PWD/jre/bin:$PATH; /bin/sh run.sh ")
    writter.print(Integer.toString(memorySizeForRuntime))
    writter.print("m ")
    writter.print(UUID.randomUUID)
    writter.print(" -s ")
    writter.print(communicationStorage.root.toString)
    writter.print(" -c ")
    writter.print(communicationDirPath)
    writter.print(" -a $CUR/authentication.xml ")
    writter.print(" -p $CUR/envplugins/ ")
    writter.print(" -i ")
    writter.print(inputFilePath)
    writter.print(" -o ")
    writter.print(resultPath)
    //    writter.print(" -w $CUR") // 2>err.txt ; lcg-cp file:$PWD/err.txt " + path.toStringURI("err.txt")) 
    writter.print("; cd .. ; rm -rf $CUR")
  }

  private def mkLcgCpGunZipCmd(env: GliteEnvironment, from: String, to: String) = {
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
    builder.append(from)
    builder.append(" file:")
    builder.append(to)
    builder.append(".gz; gunzip ")
    builder.append(to)
    builder.append(".gz; ")

    builder.toString
  }

  private def getTimeOut = Workspace.preferenceAsDurationInS(GliteEnvironment.LCGCPTimeOut).toString

  protected def buildJobDescription(runtime: Runtime, script: File) = {
    val description = newJobDescription

    description.setAttribute(JobDescription.EXECUTABLE, "/bin/bash")
    description.setVectorAttribute(JobDescription.ARGUMENTS, Array[String](script.getName))

    /*description.setAttribute(JobDescription.OUTPUT, "out.txt")
    description.setAttribute(JobDescription.ERROR, "err.txt")*/

    description.setVectorAttribute(JobDescription.FILETRANSFER, Array[String]("file:/" +
      { if (script.getAbsolutePath.startsWith("/")) script.getAbsolutePath.tail else script.getAbsolutePath } +
      ">" + script.getName))

    environment.allRequirements.get(GLITE_REQUIREMENTS) match {
      case Some(requirement) ⇒
        val requirements = new StringBuilder
        requirements.append("JDLRequirements=(")
        requirements.append(requirement)
        requirements.append(')')

        description.setVectorAttribute("Extension", Array[String](requirements.toString))
      case None ⇒
    }

    environment.authentication.myProxy match {
      case Some(myProxy) ⇒
        description.setAttribute(VOMSContext.MYPROXYSERVER, myProxy.url)
      case None ⇒
    }

    description
  }
}
