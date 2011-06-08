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

package org.openmole.plugin.environment.jsaga

import java.io.File
import java.io.PrintStream
import org.joda.time.format.ISOPeriodFormat

import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.batch.environment.Runtime
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga.JSAGAAttributes._

object JSAGAJobBuilder {

  def jobDescription(runtime: Runtime, tmpScript: File, attributes: Map[String, String]): JobDescription = {
    val description = JobFactory.createJobDescription

    description.setAttribute(JobDescription.EXECUTABLE, "/bin/bash")

    description.setVectorAttribute(JobDescription.ARGUMENTS, Array[String](tmpScript.getName))
    description.setVectorAttribute(JobDescription.FILETRANSFER, Array[String](tmpScript.toURI().toURL().toString + ">" + tmpScript.getName))

    attributes.get(CPU_TIME) match {
      case Some(value) => 
        description.setAttribute(CPU_TIME, ISOPeriodFormat.standard.parsePeriod(value).toStandardMinutes.getMinutes.toString)
      case None =>
    }

    description
  }

  lazy val helloWorld = {
    val helloFile = Workspace.newFile("testhello", ".txt")
    
    val str = new PrintStream(helloFile)
    str.println("Hello")
    str.close

    val hello = JobFactory.createJobDescription

    hello.setAttribute(JobDescription.EXECUTABLE, "/bin/echo")
    hello.setVectorAttribute(JobDescription.ARGUMENTS, Array[String]("Hello"))
    hello.setVectorAttribute(JobDescription.FILETRANSFER, Array[String](helloFile.toURI().toURL() + ">" + helloFile.getName))

    hello     
  }
  
}
