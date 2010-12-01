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

package org.openmole.plugin.environment.jsaga

import java.io.File
import java.io.PrintStream
import org.joda.time.format.ISOPeriodFormat

import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.plugin.environment.jsaga.internal.Activator
import org.openmole.core.batch.environment.Runtime
import org.openmole.plugin.environment.jsaga.JSAGAAttributes._

object JSAGAJobBuilder {

  def jobDescription(runtime: Runtime, tmpScript: File, attributes: Map[String, String]): JobDescription = {
    try {

      val description = JobFactory.createJobDescription

      description.setAttribute(JobDescription.EXECUTABLE, "/bin/bash");

      description.setVectorAttribute(JobDescription.ARGUMENTS, Array[String](tmpScript.getName))
      description.setVectorAttribute(JobDescription.FILETRANSFER, Array[String](tmpScript.toURI().toURL().toString + ">" + tmpScript.getName))

      for (attribute <- JSAGAAttributes.values) {
        attributes.get(attribute) match {
          case Some(value) => 
            description.setAttribute(attribute, if (attribute.equals(CPU_TIME)) {
                ISOPeriodFormat.standard.parsePeriod(value).toStandardSeconds.getSeconds.toString
              } else value)
          case None =>
        }
      }

      description
    } catch {
      //FIXME remove when full scala
      case e => throw new InternalProcessingError(e)
    } 
  }

  lazy val helloWorld = try {

    val helloFile = Activator.getWorkspace.newFile("testhello", ".txt")
    val str = new PrintStream(helloFile)

    str.println("Hello")
    str.close

    val hello = JobFactory.createJobDescription

    hello.setAttribute(JobDescription.EXECUTABLE, "/bin/echo")
    hello.setVectorAttribute(JobDescription.ARGUMENTS, Array[String]("Hello"))
    hello.setVectorAttribute(JobDescription.FILETRANSFER, Array[String](helloFile.toURI().toURL() /*getSchemeSpecificPart()*/ + ">" + helloFile.getName))

    hello
  } catch {
    case e => throw new InternalProcessingError(e)
  } 
              
}

