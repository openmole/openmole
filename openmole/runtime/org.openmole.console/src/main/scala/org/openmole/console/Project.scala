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

import org.openmole.tool.file._

import scala.util.{ Success, Failure, Try }

/*class Project(workDirectory: File, script: String)(
  pluginsDirectory: File = workDirectory / "plugin") {

  lazy val console = new Console()
  def loadPlugins =  if (pluginsDirectory.exists()) PluginManager.load(pluginsDirectory.listFilesSafe) else List.empty

  def run(args: Seq[String] = Seq.empty, pluginSet: PluginSet) = {
    def engine = console.newREPL(ConsoleVariables(args))
    Console.withREPL(ConsoleVariables(args, workDirectory)) { loop ⇒
      Try(loop.compile(scriptFile.content)) match {
        case Failure(e) ⇒
          println(e.getMessage)
          println(e.stackString)
          ExitCodes.compilationError
        case Success(compiled) ⇒
          compiled.eval() match {
            case res: PuzzleBuilder ⇒
              val ex = res.buildPuzzle.toExecution()
              ex.start
              Try(ex.waitUntilEnded) match {
                case Success(_) ⇒ ExitCodes.ok
                case Failure(e) ⇒
                  println("Error during script execution: " + e.getMessage)
                  print(e.stackString)
                  ExitCodes.executionError
              }
            case _ ⇒
              println(s"Script $scriptFile doesn't end with a puzzle")
              ExitCodes.notAPuzzle
          }
      }
    }


  }
}*/
