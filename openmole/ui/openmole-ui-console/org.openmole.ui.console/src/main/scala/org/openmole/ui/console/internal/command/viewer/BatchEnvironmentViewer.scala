/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.ui.console.internal.command.viewer

import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.cli.BasicParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.OptionBuilder
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.openmole.core.batch.control.BatchServiceDescription
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.model.execution.ExecutionState.ExecutionState
import scala.collection.mutable.HashMap

class BatchEnvironmentViewer extends IViewer[BatchEnvironment] {
  val environmentViewer = new EnvironmentViewer

  override def view(obj: BatchEnvironment, args: Array[String]) = {
    import IViewer.Separator
    
    val options = new Options
    options.addOption("v", true, "level of verbosity")

    try {
      val parser = new BasicParser
      val commandLine = parser.parse(options, args)

      val v = if (commandLine.hasOption('v')) {
        commandLine.getOptionValue('v').toInt
      } else 0

      environmentViewer.view(obj, args)
      if (v >= 1) {
        System.out.println(Separator)
        val jobServices = new HashMap[BatchServiceDescription, HashMap[ExecutionState, AtomicInteger]]
        val executionJobRegistry = obj.jobRegistry

        for (executionJob <-  executionJobRegistry.allExecutionJobs) {
          val batchJob = executionJob.batchJob
          if (batchJob != null) {
            val jobServiceInfo = jobServices.getOrElseUpdate(batchJob.jobServiceDescription,  new HashMap[ExecutionState, AtomicInteger])
            val nb = jobServiceInfo.getOrElseUpdate(batchJob.state, new AtomicInteger)
            nb.incrementAndGet
          }
        }


        jobServices.foreach {
          case (description, states) =>
          
            System.out.print(description.toString() + ":")
            val jobServiceInfo = jobServices.get(description) 
            states.foreach {
              case(state, nb) => System.out.print(" [" + state.name + " = " + nb + "]")
            }
            System.out.println
        }
      }

      if (v >= 2) {
        System.out.println(Separator)

        val executionJobRegistry = obj.jobRegistry
        for (executionJob <- executionJobRegistry.allExecutionJobs) {
          val batchJob = executionJob.batchJob
          if (batchJob != null) {
            System.out.println(batchJob.toString + " " + batchJob.state.toString)
          }
        }
      }
    } catch {
      case ex: ParseException =>
        Logger.getLogger(classOf[BatchEnvironmentViewer].getName).log(Level.SEVERE, "Wrong arguments format.")
        new HelpFormatter().printHelp(" ", options)
    }

  }
}
