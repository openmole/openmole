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
import org.openmole.core.console.ScalaREPL
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import org.openmole.tool.file._
import org.openmole.tool.thread._

import scala.util.{ Success, Failure, Try }

sealed trait CompileResult
case class ScriptFileDoesNotExists() extends CompileResult
case class CompilationError(exception: Throwable) extends CompileResult
case class Compiled(result: CompiledScript) extends CompileResult {
  def eval = result.eval().asInstanceOf[PuzzleBuilder]
}

object Project {
  def newREPL(variables: ConsoleVariables) = new Console().newREPL(variables, quiet = true)
}

class Project(workDirectory: File, newREPL: (ConsoleVariables) ⇒ ScalaREPL = Project.newREPL) {

  def pluginsDirectory: File = workDirectory / "plugins"
  def plugins = pluginsDirectory.listFilesSafe

  def loadPlugins = PluginManager.load(plugins)

  def compile(script: File, args: Seq[String]): CompileResult = {
    if (!script.exists) ScriptFileDoesNotExists()
    else {
      def compileContent =
        s"""${scriptsObjects(script.getParentFileSafe).mkString("\n")}
            |def runOMSScript(): ${classOf[PuzzleBuilder].getCanonicalName} = {
            |${script.content}
            |}
            |runOMSScript()
       """.stripMargin
      compile(compileContent, args)
    }
  }

  private def compile(content: String, args: Seq[String]): CompileResult = {
    val loop = newREPL(ConsoleVariables(args, workDirectory))
    try Compiled(loop.compile(content))
    catch {
      case e: Throwable ⇒ CompilationError(e)
    }
    finally loop.close()
  }

  def scriptFiles(dir: File) = dir.listFilesSafe(_.getName.endsWith(".oms"))

  def scriptsObjects(dir: File) =
    for {
      script ← scriptFiles(dir)
    } yield makeObject(script)

  def makeObject(script: File) =
    s"""
       |class ${script.getName.dropRight(".oms".size)}Class {
       |${script.content}
       |}
       |lazy val ${script.getName.dropRight(".oms".size)} = new ${script.getName.dropRight(".oms".size)}Class
     """.stripMargin

}
