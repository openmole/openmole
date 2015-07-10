/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.console

import jline.console.ConsoleReader
import java.io.File
import java.util.concurrent.atomic.{ AtomicLong, AtomicInteger }
import org.openmole.core.batch.authentication._
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workflow.execution.local._
import org.openmole.core.workflow.execution.{ Environment, ExecutionState }
import org.openmole.core.workflow.job.State
import org.openmole.core.workflow.mole.{ ExecutionContext, Mole, MoleExecution }
import org.openmole.core.workflow.transition.IAggregationTransition
import org.openmole.core.workflow.transition.IExplorationTransition
import org.openmole.core.workflow.validation.Validation
import org.openmole.core.serializer.SerialiserService
import org.openmole.core.workspace.Workspace
import scala.annotation.tailrec
import scala.collection.mutable.HashMap
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.dsl._
import Console._
import org.openmole.core.buildinfo

class Command {

  def print(environment: Environment): Unit =
    for {
      (label, number) ← List(
        "Submitted" -> environment.submitted,
        "Running" -> environment.running,
        "Done" -> environment.done,
        "Failed" -> environment.failed
      )
    } println(s"$label: $number")

  def print(mole: Mole): Unit = {
    println("root: " + mole.root)
    mole.transitions.foreach(println)
    mole.dataChannels.foreach(println)
  }

  def print(moleExecution: MoleExecution): Unit = {
    val toDisplay = Array.fill(State.values.size)(new AtomicLong)
    for (job ← moleExecution.moleJobs) toDisplay(job.state.id).incrementAndGet
    for (state ← State.values)
      state match {
        case State.COMPLETED ⇒ System.out.println(state.toString + ": " + moleExecution.completed)
        case State.FAILED    ⇒
        case State.CANCELED  ⇒
        case _               ⇒ System.out.println(state.toString + ": " + toDisplay(state.id))
      }
  }

  def verify(mole: Mole): Unit = Validation(mole).foreach(println)

  def encrypted: String = encrypt(askPassword())

  def version =
    println(s"""You are running OpenMOLE ${buildinfo.version} - ${buildinfo.name}
       |built on the ${buildinfo.generationDate}.""".stripMargin)

}

