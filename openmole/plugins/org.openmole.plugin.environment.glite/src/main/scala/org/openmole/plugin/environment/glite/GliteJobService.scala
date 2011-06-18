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
import org.ogf.saga.job.JobDescription
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.batch.environment.Runtime
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga.JSAGAJob
import org.openmole.plugin.environment.jsaga.JSAGAJobService

import scala.collection.JavaConversions._

object GliteJobService {
  val ConfigGroup = GliteJobService.getClass.getSimpleName
  val LCGCPTimeOut = new ConfigurationLocation(ConfigGroup, "RuntimeCopyOnWNTimeOut")
    
  Workspace += (LCGCPTimeOut, "PT2M")
}


class GliteJobService(jobServiceURI: URI, environment: GliteEnvironment, nbAccess: Int) extends JSAGAJobService(jobServiceURI, environment, nbAccess)  {

  override protected def buildJob(id: String, resultPath: String) = new GliteJob(id, resultPath, this, environment.authentication.expires)

  override protected def generateScriptString(serializedJob: SerializedJob, resultPath: String, memorySizeForRuntime: Int, os: OutputStream) = {
    import serializedJob.communicationStorage.stringDecorator
    import serializedJob._
    
    val writter = new PrintStream(os)
    
    assert(runtime.runtime.path != null)
    writter.print("BASEPATH=$PWD;CUR=$PWD/ws$RANDOM;while test -e $CUR; do CUR=$PWD/ws$RANDOM;done;mkdir $CUR; export HOME=$CUR; cd $CUR; ")
    writter.print(mkLcgCpGunZipCmd(environment, runtime.runtime.path.toStringURI, "$PWD/openmole.tar.bz2"))
    writter.print(" tar -xjf openmole.tar.bz2 >/dev/null; rm -f openmole.tar.bz2; ")
    writter.print("mkdir envplugins; PLUGIN=0;");

    for (plugin <- runtime.environmentPlugins) {
      assert(plugin.path != null)
      writter.print(mkLcgCpGunZipCmd(environment, plugin.path.toStringURI, "$CUR/envplugins/plugin$PLUGIN.jar"))
      writter.print("PLUGIN=`expr $PLUGIN + 1`; ")
    }
    
    assert(runtime.authentication.path != null)
    writter.print(mkLcgCpGunZipCmd(environment, runtime.authentication.path.toStringURI, "$CUR/authentication.xml"))

    writter.print("cd org.openmole.runtime-*; export PATH=$PWD/jre/bin:$PATH; /bin/sh run.sh ")
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
    builder.append(".gz;")

    builder.toString
  }

  private def getTimeOut = Workspace.preferenceAsDurationInS(GliteJobService.LCGCPTimeOut).toString
  

  override protected def buildJobDescription(runtime: Runtime, script: File,  attributes: Map[String, String]) = {
    val description = super.buildJobDescription(runtime, script, attributes)

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
