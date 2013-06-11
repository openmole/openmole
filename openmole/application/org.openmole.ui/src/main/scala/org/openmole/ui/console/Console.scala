/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ui.console

import java.util.concurrent.Executors
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.logging.LoggerService
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.script.ScalaREPL
import scala.annotation.tailrec
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.ILoop
import scala.tools.nsc.interpreter.JLineCompletion
import scala.tools.nsc.interpreter.JLineReader
import org.openmole.core.model.task._
import java.util.concurrent.TimeUnit
import scala.tools.nsc.io.{ File ⇒ SFile }
import java.io.File

class Console(plugins: PluginSet, password: Option[String], script: Option[String]) { console ⇒

  def setPassword(password: String) =
    try {
      Workspace.setPassword(password)
      true
    }
    catch {
      case e: UserBadDataError ⇒
        println("Password incorrect.")
        false
    }

  @tailrec private def initPassword: Unit = {
    val message = (if (Workspace.passwordChosen) "Enter your OpenMOLE password" else "OpenMOLE Password has not been set yet, choose a  password") + "  (for preferences encryption):"
    val password = new jline.ConsoleReader().readLine(message, '*')
    if (!setPassword(password)) initPassword
  }

  def workspace = "workspace"
  def registry = "registry"
  def logger = "logger"
  def serializer = "serializer"

  def run {
    val correctPassword =
      password match {
        case None    ⇒ initPassword; true
        case Some(p) ⇒ setPassword(p)
      }

    if (correctPassword) {
      val loop = new ScalaREPL

      try {
        loop.beQuietDuring {
          loop.bind(workspace, Workspace)
          loop.bind(logger, LoggerService)
          loop.bind(serializer, new Serializer)
          loop.bind("commands", new Command)
          loop.bind("implicits", new Implicits()(plugins))
          loop.addImports(
            "org.openmole.core.implementation.data._",
            "org.openmole.core.implementation.execution._",
            "org.openmole.core.implementation.execution.local._",
            "org.openmole.core.implementation.job._",
            "org.openmole.core.implementation.mole._",
            "org.openmole.core.implementation.sampling._",
            "org.openmole.core.implementation.task._",
            "org.openmole.core.implementation.transition._",
            "org.openmole.core.implementation.tools._",
            "org.openmole.core.implementation.puzzle._",
            "org.openmole.core.batch.authentication._",
            "org.openmole.core.model.data._",
            "org.openmole.core.model.transition._",
            "org.openmole.core.model.mole._",
            "org.openmole.core.model.sampling._",
            "org.openmole.core.model.task._",
            "org.openmole.core.batch.authentication._",
            "org.openmole.misc.workspace._",
            "org.openmole.misc.tools.io.FromString._",
            "java.io.File",
            "commands._",
            "implicits._")

        }

        script match {
          case None ⇒ loop.loop
          case Some(s) ⇒
            val scriptFile = new File(s)
            if (scriptFile.exists) loop.interpretAllFrom(new SFile(scriptFile))
            else println("File " + scriptFile + " doesn't exist.")
        }
      }
      finally loop.close
    }
  }

  //def setVariable(name: String, value: Object) = binding.setVariable(name, value)

  //def run(command: String) = groovysh.run(command)

  //def leftShift(cmnd: Command): Object = groovysh.leftShift(cmnd)

}
