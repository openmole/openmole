/**
 * Created by Romain Reuillon on 22/01/16.
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
 *
 */
package org.openmole.core.project

import javax.script.CompiledScript

import monocle.function.Each
import org.openmole.core.console._
import org.openmole.core.pluginmanager._
import org.openmole.core.project.Imports.Tree
import org.openmole.core.workflow.puzzle._
import org.openmole.tool.file._
import monocle.function.all._
import monocle.std.all._

object Project {
  def scriptExtension = ".oms"
  def isScript(file: File) = file.exists() && file.getName.endsWith(scriptExtension)
  def newREPL(variables: ConsoleVariables) = OpenMOLEREPL.newREPL(variables, quiet = true)

  def scriptsObjects(script: File) = makeScript(Imports.importTree(script))

  def makeScript(tree: Tree): String =
    (tree.files.map(makeObject) ++ tree.children.map(c ⇒ makePackage(c.name, c.tree))).mkString("\n")

  def makePackage(name: String, tree: Tree): String =
    s"""object $name {
     |${tree.children.map(c ⇒ makeScript(tree)).mkString("\n")}
     |}
   """.stripMargin

  def makeObject(script: File): String =
    s"""class ${script.getName.dropRight(Project.scriptExtension.size)}Class {
       |${script.content}
       |}
       |
       |lazy val ${script.getName.dropRight(Project.scriptExtension.size)} = new ${script.getName.dropRight(".oms".size)}Class
     """.stripMargin

}

sealed trait CompileResult
case class ScriptFileDoesNotExists() extends CompileResult
case class CompilationError(exception: Throwable) extends CompileResult
case class Compiled(result: CompiledScript) extends CompileResult {
  def eval = result.eval().asInstanceOf[Puzzle]
}

class Project(workDirectory: File, newREPL: (ConsoleVariables) ⇒ ScalaREPL = Project.newREPL) {

  def pluginsDirectory: File = workDirectory / "plugins"
  def plugins = pluginsDirectory.listFilesSafe

  def loadPlugins = PluginManager.load(plugins)

  def compile(script: File, args: Seq[String]): CompileResult = {
    if (!script.exists) ScriptFileDoesNotExists()
    else {
      def header =
        s"""${scriptsObjects(script)}
           |
           |def runOMSScript(): ${classOf[Puzzle].getCanonicalName} = {""".stripMargin

      def footer =
        s"""}
           |runOMSScript()""".stripMargin

      def compileContent =
        s"""$header
           |${script.content}
           |$footer""".stripMargin

      def compile(content: String, args: Seq[String]): CompileResult = {
        val loop = newREPL(ConsoleVariables(args, workDirectory))
        try Compiled(loop.compile(content))
        catch {
          case ce: ScalaREPL.CompilationError ⇒
            def positionLens =
              ScalaREPL.CompilationError.errorMessages composeTraversal
                each composeLens
                ScalaREPL.ErrorMessage.position composePrism
                some

            def headerOffset = header.size + 1

            import ScalaREPL.ErrorPosition

            def adjusted =
              (positionLens composeLens ErrorPosition.line modify { _ - header.split("\n").size }) andThen
                (positionLens composeLens ErrorPosition.start modify { _ - headerOffset }) andThen
                (positionLens composeLens ErrorPosition.end modify { _ - headerOffset })

            CompilationError(adjusted(ce))
          case e: Throwable ⇒ CompilationError(e)
        }
        finally loop.close()
      }

      compile(compileContent, args)
    }
  }

  def scriptsObjects(script: File) = Project.scriptsObjects(script)

}
