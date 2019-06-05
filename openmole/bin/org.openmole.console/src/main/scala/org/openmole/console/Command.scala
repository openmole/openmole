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

import java.io.{ File, IOException, StringReader }
import java.util.logging.Level

import org.openmole.core.buildinfo
import org.openmole.core.console.ScalaREPL
import org.openmole.core.dsl._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.FileService
import org.openmole.core.project._
import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workflow.execution.Environment
import org.openmole.core.workflow.mole.{ Mole, MoleExecution }
import org.openmole.core.workflow.validation.Validation
import org.openmole.core.module
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.preference.Preference
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.core.services._
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.random.{ RandomProvider, Seeder }

class Command(val console: ScalaREPL, val variables: ConsoleVariables) { commands ⇒

  def print(environment: Environment): Unit = {
    for {
      (label, number) ← List(
        "Submitted" → environment.submitted,
        "Running" → environment.running,
        "Done" → environment.done,
        "Failed" → environment.failed
      )
    } println(s"$label: $number")
    val errors = Environment.errors(environment)
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
    val statuses = moleExecution.capsuleStatuses

    val msg =
      for {
        (capsule, stat) ← statuses
      } yield s"${capsule}: ${stat.ready} ready, ${stat.running} running, ${stat.completed} completed"

    println(msg.mkString("\n"))
    moleExecution.exception match {
      case Some(e) ⇒
        MoleExecution.MoleExecutionFailed.capsule(e) match {
          case Some(c) ⇒ System.out.println(s"Mole execution failed while executing ${c}:")
          case None    ⇒ System.out.println(s"Mole execution failed:")
        }
        System.out.println(exceptionToString(e.exception))
      case None ⇒
    }
  }

  private def exceptionToString(e: Throwable) = e.stackString

  implicit def stringToLevel(s: String) = Level.parse(s.toUpperCase)

  def errors(environment: Environment, level: Level = Level.INFO) = {
    def filtered =
      Environment.clearErrors(environment).filter {
        e ⇒ e.level.intValue() >= level.intValue()
      }

    for {
      error ← filtered
    } println(s"${error.level.toString}: ${exceptionToString(error.exception)}")
  }

  def verify(mole: Mole)(implicit newFile: NewFile, fileService: FileService): Unit = Validation(mole).foreach(println)

  def encrypted(implicit cypher: Cypher): String = encrypt(Console.askPassword())

  def version() =
    println(s"""You are running OpenMOLE ${buildinfo.version} - ${buildinfo.name}
       |built on the ${buildinfo.version.generationDate}.""".stripMargin)

  def loadAny(file: File, args: Seq[String] = Seq.empty)(implicit services: Services): Any =
    try {
      val newRepl =
        (v: ConsoleVariables) ⇒ {
          ConsoleVariables.bindVariables(console, v)
          console
        }

      Project.compile(variables.workDirectory, file, args, newREPL = Some(newRepl)) match {
        case ScriptFileDoesNotExists() ⇒ throw new IOException("File " + file + " doesn't exist.")
        case e: CompilationError       ⇒ throw e.error
        case Compiled(compiled)        ⇒ compiled.apply()
      }
    }
    finally ConsoleVariables.bindVariables(console, variables)

  def load(file: File, args: Seq[String] = Seq.empty)(implicit services: Services): DSL =
    loadAny(file) match {
      case res: DSL ⇒ res
      case x        ⇒ throw new UserBadDataError("The result is not a puzzle")
    }

  def modules(urls: OptionalArgument[Seq[String]] = None)(implicit preference: Preference, randomProvider: RandomProvider, newFile: NewFile, fileService: FileService): Unit = {
    val installedBundles = PluginManager.bundleHashes.map(_.toString).toSet
    def installed(components: Seq[String]) = (components.toSet -- installedBundles).isEmpty

    urls.getOrElse(module.indexes).flatMap {
      url ⇒
        module.modules(url).map {
          m ⇒
            def installedString = if (installed(m.components.map(_.hash))) " (installed)" else ""
            m.name + installedString
        }
    }.sorted.foreach(println)
  }

  def install(name: String*)(implicit preference: Preference, randomProvider: RandomProvider, newFile: NewFile, workspace: Workspace, fileService: FileService): Unit = install(name)
  def install(names: Seq[String], urls: OptionalArgument[Seq[String]] = None)(implicit preference: Preference, randomProvider: RandomProvider, newFile: NewFile, workspace: Workspace, fileService: FileService): Unit = {
    val toInstall = urls.getOrElse(module.indexes).flatMap(url ⇒ module.selectableModules(url)).filter(sm ⇒ names.contains(sm.module.name))
    if (toInstall.isEmpty) println("The module(s) is/are already installed.")
    else
      Console.dealWithLoadError(module.install(toInstall), interactive = true) match {
        case Seq() ⇒
          println("The module(s) has/have been successfully installed, please restart the console to enable it/them.")
        case e ⇒
          println("There was some errors during the installation, please restart the console to enable the installed module(s).")
      }
  }

}

