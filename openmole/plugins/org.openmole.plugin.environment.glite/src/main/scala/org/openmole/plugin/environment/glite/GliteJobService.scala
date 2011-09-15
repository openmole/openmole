/*
 * Copyright (C) 2010 reuillon
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
import org.ogf.saga.job.Job
import org.ogf.saga.job.JobDescription
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.misc.exception.InternalProcessingError
import org.ogf.saga.job.JobFactory
import org.openmole.core.batch.environment.Runtime
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga.JSAGAJob
import org.openmole.plugin.environment.jsaga.JSAGAJobBuilder
import org.openmole.plugin.environment.jsaga.JSAGAJobService
import org.openmole.core.batch.control.AccessToken
import org.openmole.misc.tools.io.FileUtil._
import scala.collection.JavaConversions._

object GliteJobService {
  val ConfigGroup = GliteJobService.getClass.getSimpleName
  val LCGCPTimeOut = new ConfigurationLocation(ConfigGroup, "RuntimeCopyOnWNTimeOut")
    
  Workspace += (LCGCPTimeOut, "PT2M")
}


class GliteJobService(jobServiceURI: URI, environment: GliteEnvironment, nbAccess: Int) extends JSAGAJobService(jobServiceURI, environment, nbAccess)  {

  override protected def doSubmit(serializedJob: SerializedJob, token: AccessToken) = {
    import serializedJob._
    import communicationStorage.stringDecorator
    
    val script = Workspace.newFile("script", ".sh")
    try {
      val outputFilePath = communicationDirPath.toURIFile.newFileInDir("job", ".out").path
   
      val os = script.bufferedOutputStream
      try generateScript(serializedJob, outputFilePath, environment.memorySizeForRuntime.intValue, os)
      finally os.close
      
      //logger.fine(fromFile(script).getLines.mkString)
      
      val jobDescription = buildJobDescription(runtime, script, environment.attributes)
      val job = jobServiceCache.createJob(jobDescription)
      job.run
            
      val id = job.getAttribute(Job.JOBID)
      val idStr = id.substring(id.lastIndexOf('[') + 1, id.lastIndexOf(']'))
      new GliteJob(idStr, outputFilePath, this, environment.authentication.expires)
    } finally script.delete
  }
  
  protected def generateScript(serializedJob: SerializedJob, resultPath: String, memorySizeForRuntime: Int, os: OutputStream) = {
    import serializedJob.communicationStorage.stringDecorator
    import serializedJob._
    
    val writter = new PrintStream(os)
    
    assert(runtime.runtime.path != null)
    writter.print("BASEPATH=$PWD;CUR=$PWD/ws$RANDOM;while test -e $CUR; do CUR=$PWD/ws$RANDOM;done;mkdir $CUR; export HOME=$CUR; cd $CUR; ")
    writter.print(mkLcgCpGunZipCmd(environment, runtime.jvm.path.toStringURI, "$PWD/jvm.tar.gz"))
    writter.print("tar -xzf jvm.tar.gz >/dev/null; rm -f jvm.tar.gz; ")
    writter.print(mkLcgCpGunZipCmd(environment, runtime.runtime.path.toStringURI, "$PWD/openmole.tar.gz"))
    writter.print("tar -xzf openmole.tar.gz >/dev/null; rm -f openmole.tar.gz; ")
    writter.print("mkdir envplugins; PLUGIN=0;");

    for (plugin <- runtime.environmentPlugins) {
      assert(plugin.path != null)
      writter.print(mkLcgCpGunZipCmd(environment, plugin.path.toStringURI, "$CUR/envplugins/plugin$PLUGIN.jar"))
      writter.print("PLUGIN=`expr $PLUGIN + 1`; ")
    }
    
    assert(runtime.authentication.path != null)
    writter.print(mkLcgCpGunZipCmd(environment, runtime.authentication.path.toStringURI, "$CUR/authentication.xml"))

    writter.print(" export PATH=$PWD/jre/bin:$PATH; /bin/sh run.sh ")
    writter.print(Integer.toString(memorySizeForRuntime))
    writter.print("m ")
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
    writter.print(" -w $CUR ; cd .. ; rm -rf $CUR")
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

  private def getTimeOut = Workspace.preferenceAsDurationInS(GliteJobService.LCGCPTimeOut).toString
  
  protected def buildJobDescription(runtime: Runtime, script: File,  attributes: Map[String, String]) = {
    val description = JSAGAJobBuilder.description(attributes)

    description.setAttribute(JobDescription.EXECUTABLE, "/bin/bash")
    description.setVectorAttribute(JobDescription.ARGUMENTS, Array[String](script.getName))
 
    description.setVectorAttribute(JobDescription.FILETRANSFER, Array[String]("file:/" + 
                                                                              {if(script.getAbsolutePath.startsWith("/")) script.getAbsolutePath.tail else script.getAbsolutePath} +
                                                                              ">" + script.getName))

    attributes.get(GliteAttributes.REQUIREMENTS) match {
      case Some(requirement) => val requirements = new StringBuilder
        requirements.append("JDLRequirements=(")
        requirements.append(requirement)
        requirements.append(')')

        description.setVectorAttribute("Extension", Array[String](requirements.toString))
      case None =>
    }

    description
  }
}
