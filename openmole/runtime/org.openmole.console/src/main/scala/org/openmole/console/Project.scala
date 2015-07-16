/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.console

import javax.script.CompiledScript
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workflow.puzzle._
import org.openmole.tool.file._

import scala.util.{ Success, Failure, Try }

sealed trait CompileResult
case class ScriptFileDoesNotExists() extends CompileResult
case class CompilationError(exception: Throwable) extends CompileResult
case class Compiled(result: CompiledScript) extends CompileResult

class Project(workDirectory: File) {

  def pluginsDirectory: File = workDirectory / "plugins"
  def plugins = pluginsDirectory.listFilesSafe

  lazy val console = new Console()
  def loadPlugins = PluginManager.load(plugins)

  def compile(script: File, args: Seq[String]): CompileResult = {
    if (!script.exists) ScriptFileDoesNotExists()
    else {
      def compileContent =
        s"""
            |object oms {
            |${scriptsObjects(script.getParentFileSafe).mkString("\n")}
            |}
            |import oms._
            |def run() = {
            |${script.content}
            |}
            |run()
       """.stripMargin
      compile(compileContent, args)
    }
  }

  private def compile(content: String, args: Seq[String]): CompileResult = {
    console.withREPL(ConsoleVariables(args, workDirectory)) { loop ⇒
      Try(loop.compile(content)) match {
        case Failure(e)        ⇒ CompilationError(e)
        case Success(compiled) ⇒ Compiled(compiled)
      }
    }
  }

  def scriptFiles(dir: File) = dir.listFilesSafe(_.getName.endsWith(".oms"))

  def scriptsObjects(dir: File) =
    for {
      script ← scriptFiles(dir)
    } yield makeObject(script)

  def makeObject(script: File) =
    s"""
       |lazy val ${script.getName.dropRight(".oms".size)} = new {
       |${script.content}
       |}
     """.stripMargin

}
