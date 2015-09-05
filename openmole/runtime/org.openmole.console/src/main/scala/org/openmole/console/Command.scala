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

import java.util.logging.Level

import jline.console.ConsoleReader
import java.io.{ IOException, PrintWriter, StringWriter, File }
import java.util.concurrent.atomic.{ AtomicLong, AtomicInteger }
import org.apache.log4j.lf5.viewer.LogTableColumn
import org.openmole.core.batch.authentication._
import org.openmole.core.console.ScalaREPL
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workflow.execution.local._
import org.openmole.core.workflow.execution.{ Environment, ExecutionState }
import org.openmole.core.workflow.job.State
import org.openmole.core.workflow.mole.{ ExecutionContext, Mole, MoleExecution }
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.validation.Validation
import org.openmole.core.serializer.SerialiserService
import org.openmole.core.workspace.Workspace
import scala.annotation.tailrec
import scala.collection.mutable.HashMap
import org.openmole.core.workflow.mole._
import org.openmole.core.dsl._
import Console._
import org.openmole.core.buildinfo
import org.openmole.core.tools.io.Prettifier._

class Command(val console: ScalaREPL, val variables: ConsoleVariables) { commands ⇒

  def print(environment: Environment): Unit = {
    for {
      (label, number) ← List(
        "Submitted" -> environment.submitted,
        "Running" -> environment.running,
        "Done" -> environment.done,
        "Failed" -> environment.failed
      )
    } println(s"$label: $number")
    val errors = environment.errors
    def low = errors.count(_.level.intValue() <= Level.INFO.intValue())
    def warning = errors.count(_.level.intValue() == Level.WARNING.intValue())
    def severe = errors.count(_.level.intValue() == Level.SEVERE.intValue())
    println(s"$severe critical errors, $warning warning and $low low-importance errors. Use the errors() function to display them.")
  }

  def print(mole: Mole): Unit = {
    println("root: " + mole.root)
    mole.transitions.foreach(println)
    mole.dataChannels.foreach(println)
  }

  def print(moleExecution: MoleExecution): Unit = {
    val statuses = moleExecution.jobStatuses
    println(s"Ready: ${statuses.ready}")
    println(s"Running: ${statuses.running}")
    println(s"Completed: ${statuses.completed}")
    moleExecution.exception match {
      case Some(e) ⇒
        System.out.println(s"Mole execution failed while executing ${e.capsule}:")
        System.out.println(exceptionToString(e.exception))
      case None ⇒
    }
  }

  private def exceptionToString(e: Throwable) = e.stackString

  implicit def stringToLevel(s: String) = Level.parse(s.toUpperCase)

  def errors(environment: Environment, level: Level = Level.INFO) = {
    def filtered = environment.readErrors.filter {
      e ⇒ e.level.intValue() >= level.intValue()
    }

    for {
      error ← filtered
    } println(s"${error.level.toString}: ${exceptionToString(error.exception)}")
  }

  def verify(mole: Mole): Unit = Validation(mole).foreach(println)

  def encrypted: String = encrypt(askPassword())

  def version() =
    println(s"""You are running OpenMOLE ${buildinfo.version} - ${buildinfo.name}
       |built on the ${buildinfo.generationDate}.""".stripMargin)

  def loadAny(file: File, args: Seq[String] = Seq.empty): AnyRef =
    try {
      val project =
        new Project(
          variables.workDirectory,
          (v: ConsoleVariables) ⇒ {
            ConsoleVariables.bindVariables(console, v)
            console
          }
        )
      project.compile(file, args) match {
        case ScriptFileDoesNotExists() ⇒ throw new IOException("File " + file + " doesn't exist.")
        case CompilationError(e)       ⇒ throw e
        case Compiled(compiled)        ⇒ compiled.eval()
      }
    }
    finally ConsoleVariables.bindVariables(console, variables)

  def load(file: File, args: Seq[String] = Seq.empty): Puzzle =
    loadAny(file) match {
      case res: PuzzleBuilder ⇒ res.buildPuzzle
      case x                  ⇒ throw new UserBadDataError("The result is not a puzzle")
    }

}

