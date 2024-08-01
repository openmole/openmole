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

import java.io.{File, IOException, StringReader}
import java.util.logging.Level
import org.openmole.core.buildinfo
import org.openmole.core.compiler.*
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.{FileService, FileServiceCache}
import org.openmole.core.project.*
import org.openmole.core.tools.io.Prettifier.*
import org.openmole.core.workflow.execution.Environment
import org.openmole.core.workflow.mole.{Mole, MoleExecution, MoleServices}
import org.openmole.core.workflow.validation.Validation
import org.openmole.core.module
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.preference.Preference
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.core.services.*
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.random.{RandomProvider, Seeder}
import org.openmole.tool.file.*
import org.jline.terminal.Terminal

object Command:
  def start(dsl: DSL, compilationContext: CompilationContext)(implicit services: Services): MoleExecution =
    val runServices =
      import services._
      Services.copy(services)(fileServiceCache = FileServiceCache())

    import runServices._
    val moleServices = MoleServices.create(applicationExecutionDirectory = services.workspace.tmpDirectory, compilationContext = Some(compilationContext))

    val ex = MoleExecution(dsl)(moleServices)
    ex.start(true)
  end start

  def load(variables: ConsoleVariables, file: File, args: Seq[String] = Seq.empty)(implicit services: Services): Console.CompiledDSL =
    def loadAny(file: File, args: Seq[String] = Seq.empty)(implicit services: Services) =
      Project.compile(variables.workDirectory, file) match
        case ScriptFileDoesNotExists() ⇒ throw new IOException("File " + file + " doesn't exist.")
        case e: CompilationError ⇒ throw e.error
        case compiled: Compiled ⇒
          util.Try(compiled.eval(args)) match
            case util.Success(res) ⇒ Console.CompiledDSL(res, compiled.compilationContext, compiled.result)
            case util.Failure(e) ⇒ throw UserBadDataError(s"Error during evaluation of the script $file", e)

    loadAny(file)
  end load


class Command(val console: REPL, val variables: ConsoleVariables, val terminal: Terminal) { commands ⇒

  given Terminal = terminal

  def print(m: Mole): Unit = mole.print(m)
  def print(moleExecution: MoleExecution, debug: Boolean = false) = mole.print(moleExecution, debug)
  def load(file: File, args: Seq[String] = Seq.empty)(using Services): Console.CompiledDSL = mole.load(file, args)
  def start(dsl: DSL)(using Services): MoleExecution = mole.start(dsl)
  def start(dsl: Console.CompiledDSL)(using Services): MoleExecution = mole.start(dsl)
  def validate(mole: Mole)(using TmpDirectory, FileService): Unit = verify(mole)
  def verify(m: Mole)(using TmpDirectory, FileService): Unit = mole.verify(m)

  def encrypted(implicit cypher: Cypher): String = encrypt(Console.askPassword())

  export openmole.{version}

  object mole:

    def print(mole: Mole): Unit =
      println("root: " + mole.root)
      mole.transitions.foreach(println)
      mole.dataChannels.foreach(println)

    def print(moleExecution: MoleExecution, debug: Boolean = false): Unit =
      def environmentErrors(environment: Environment, level: Level) =
        def filtered =
          Environment.clearErrors(environment).filter: e ⇒
            e.level.intValue() >= level.intValue()

        for
          error ← filtered
        do
          def detail =
            error.detail match
              case None    ⇒ ""
              case Some(m) ⇒ s"\n$m\n"

          println(
            s"""${error.level.toString}: ${error.exception.getMessage}$detail
               |${exceptionToString(error.exception)}""".stripMargin
          )

      def printEnvironment(environment: Environment, debug: Boolean): Unit =
        println(environment.simpleName + ":")
        for
          (label, number) ← List(
            "Submitted" → environment.submitted,
            "Running" → environment.running,
            "Done" → environment.done,
            "Failed" → environment.failed
          )
        do println(s"  $label: $number")
        val errors = Environment.errors(environment)
        def low = errors.count(_.level.intValue() <= Level.INFO.intValue())
        def warning = errors.count(_.level.intValue() == Level.WARNING.intValue())
        def severe = errors.count(_.level.intValue() == Level.SEVERE.intValue())
        println(s"$severe critical errors, $warning warning and $low low-importance errors. Use the errors() function to display them.")
        environmentErrors(environment, (if(debug) Level.FINE else Level.INFO))

      println("\n--- Execution ---\n")

      val statuses = moleExecution.capsuleStatuses

      val msg =
        for (capsule, stat) ← statuses
        yield s"${capsule}: ${stat.ready} ready, ${stat.running} running, ${stat.completed} completed"

      println(msg.mkString("\n"))

      println("\n--- Errors ---\n")

      moleExecution.exception match
        case Some(e) ⇒
          MoleExecution.MoleExecutionFailed.capsule(e) match
            case Some(c) ⇒ System.out.println(s"Mole execution failed while executing ${c}:")
            case None    ⇒ System.out.println(s"Mole execution failed:")

          System.out.println(exceptionToString(e.exception))
        case None ⇒


      println("\n--- Environments ---\n")

      for
        env <- moleExecution.allEnvironments
      do
        printEnvironment(env, debug = debug)
        println()

    def load(file: File, args: Seq[String] = Seq.empty)(implicit services: Services): Console.CompiledDSL =
      Command.load(variables, file, args)

    def start(dsl: DSL)(implicit services: Services): MoleExecution = Command.start(dsl, CompilationContext(console.classDirectory, console.classLoader))
    def start(dsl: Console.CompiledDSL)(implicit services: Services): MoleExecution = Command.start(dsl.dsl, dsl.compilationContext)

    private def exceptionToString(e: Throwable) = e.stackString

    implicit def stringToLevel(s: String): Level = Level.parse(s.toUpperCase)

    def validate(mole: Mole)(implicit newFile: TmpDirectory, fileService: FileService): Unit = verify(mole)
    def verify(mole: Mole)(implicit newFile: TmpDirectory, fileService: FileService): Unit =
      given KeyValueCache = KeyValueCache()
      Validation(mole).foreach(println)

  object openmole:
    def version =
      println(s"""You are running OpenMOLE ${buildinfo.version} - ${buildinfo.name}
         |built on the ${buildinfo.version.generationDate}.""".stripMargin)

  object module:
    def list(urls: OptionalArgument[Seq[String]] = None)(implicit preference: Preference, randomProvider: RandomProvider, newFile: TmpDirectory, fileService: FileService): Unit =
      val installedBundles = PluginManager.bundleHashes.map(_.toString).toSet
      def installed(components: Seq[String]) = (components.toSet -- installedBundles).isEmpty

      urls.getOrElse(org.openmole.core.module.indexes).flatMap: url ⇒
        org.openmole.core.module.modules(url).map: m ⇒
          def installedString = if (installed(m.components.map(_.hash))) " (installed)" else ""
          m.name + installedString

      .sorted.foreach(println)

    def install(name: String*)(implicit preference: Preference, randomProvider: RandomProvider, newFile: TmpDirectory, workspace: Workspace, fileService: FileService): Unit = install(name)
    def install(names: Seq[String], urls: OptionalArgument[Seq[String]] = None)(implicit preference: Preference, randomProvider: RandomProvider, newFile: TmpDirectory, workspace: Workspace, fileService: FileService): Unit =
      val toInstall = urls.getOrElse(org.openmole.core.module.indexes).flatMap(url ⇒ org.openmole.core.module.selectableModules(url)).filter(sm ⇒ names.contains(sm.module.name))
      if toInstall.isEmpty
      then println("The module(s) is/are already installed.")
      else
        Console.dealWithLoadError(org.openmole.core.module.install(toInstall), interactive = true) match
          case Seq() ⇒ println("The module(s) has/have been successfully installed, please restart the console to enable it/them.")
          case e ⇒ println("There was some errors during the installation, please restart the console to enable the installed module(s).")


}

