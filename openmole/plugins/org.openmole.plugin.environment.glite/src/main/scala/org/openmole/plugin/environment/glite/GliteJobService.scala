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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.glite

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URI
import org.ogf.saga.job.JobDescription
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.batch.environment.Runtime
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.plugin.environment.glite.internal.Activator._
import org.openmole.plugin.environment.jsaga.JSAGAJob
import org.openmole.plugin.environment.jsaga.JSAGAJobService

import scala.collection.JavaConversions._

object GliteJobService {
  val ConfigGroup = GliteJobService.getClass.getSimpleName
  val LCGCPTimeOut = new ConfigurationLocation(ConfigGroup, "RuntimeCopyOnWNTimeOut")
    
  workspace += (LCGCPTimeOut, "PT2M")
}


class GliteJobService(jobServiceURI: URI, environment: GliteEnvironment, authenticationKey: GliteAuthenticationKey, authentication: GliteAuthentication, nbAccess: Int) extends JSAGAJobService(jobServiceURI, environment, authenticationKey, authentication, nbAccess)  {

  override protected def buildJob(id: String): JSAGAJob = {
    new GliteJob(id, this, super.authentication.expires)
  }

  override protected def generateScriptString(in: String, out: String, runtime: Runtime, memorySizeForRuntime: Int, os: OutputStream) = {
    val writter = new PrintStream(os)
    
    assert(runtime.runtime.location != null)
    writter.print("BASEPATH=$PWD;CUR=$PWD/ws$RANDOM;while test -e $CUR; do CUR=$PWD/ws$RANDOM;done;mkdir $CUR; export HOME=$CUR; cd $CUR; ")
    writter.print(mkLcgCpGunZipCmd(environment, runtime.runtime.location, "$PWD/openmole.tar.bz2"))
    writter.print(" tar -xjf openmole.tar.bz2 >/dev/null; rm -f openmole.tar.bz2; ")
    writter.print("mkdir envplugins; PLUGIN=0;");

    for (plugin <- runtime.environmentPlugins) {
      assert(plugin.location != null)
      writter.print(mkLcgCpGunZipCmd(environment, plugin.location, "$CUR/envplugins/plugin$PLUGIN.jar"))
      writter.print("PLUGIN=`expr $PLUGIN + 1`; ")
    }
    
    assert(runtime.authentication.location != null)
    writter.print(mkLcgCpGunZipCmd(environment, runtime.authentication.location, "$CUR/authentication.xml"))

    writter.print("cd org.openmole.runtime-*; export PATH=$PWD/jre/bin:$PATH; /bin/sh run.sh ")
    writter.print(Integer.toString(memorySizeForRuntime))
    writter.print("m ")
    writter.print("-a $CUR/authentication.xml ")
    writter.print("-p $CUR/envplugins/ ")
    writter.print("-i ")
    writter.print(in)
    writter.print(" -o ")
    writter.print(out)
    writter.print(" -w $CUR ; cd .. ; rm -rf $CUR")

  }

  private def mkLcgCpGunZipCmd(env: GliteEnvironment, from: String, to: String): String = {
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

  private def getTimeOut: String = workspace.preferenceAsDurationInS(GliteJobService.LCGCPTimeOut).toString
  

  override protected def buildJobDescription(runtime: Runtime, script: File,  attributes: Map[String, String]): JobDescription = {
    try {
      val description = super.buildJobDescription(runtime, script, attributes)

      attributes.get(GliteAttributes.REQUIREMENTS) match {
        case Some(requirement) => val requirements = new StringBuilder
          requirements.append("JDLRequirements=(")
          requirements.append(requirement)
          requirements.append(')')

          description.setVectorAttribute("Extension", Array[String](requirements.toString))
        case None =>
      }

      return description;
    } catch {
      //FIXME??remove when full scala
      case ex => throw new InternalProcessingError(ex)
    }
  }
}
