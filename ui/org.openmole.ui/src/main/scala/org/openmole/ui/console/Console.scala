/*
 * Copyright (C) 2011 reuillon
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

import org.apache.clerezza.scala.console.Interpreter
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.logging.LoggerService
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.workspace.Workspace
import scala.annotation.tailrec
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.ILoop
import scala.tools.nsc.interpreter.JLineCompletion
import scala.tools.nsc.interpreter.JLineReader


class Console {

 
  
  @tailrec private def initPassword: Unit = {
    val message = (if(Workspace.passwordChoosen) "Enter your OpenMOLE password" else "OpenMOLE Password has not been set yet, choose a  password") + "  (for preferences encryption):"
  
    val password = new jline.ConsoleReader().readLine(message, '*')
    val success = try {
      Workspace.password_=(password)
      true
    } catch {
      case e: UserBadDataError => 
        println("Password incorrect.")
        false
    }
    if(!success) initPassword
  }
  
  def pluginManager = "plugin"
  def workspace = "workspace"
  def registry = "registry"
  def logger = "logger"
  def serializer = "serializer"
  
  def run {
    initPassword
    
    val loop = new ILoop {
      override val prompt = "OpenMOLE>"
    }
    
    loop.settings = new Settings()
    loop.settings.processArgumentString("-Yrepl-sync")
    loop.in = new JLineReader(new JLineCompletion(loop))
    loop.intp = new Interpreter
    
    //loop.bind(pluginManager, PluginManager)
    loop.beQuietDuring { 
      loop.bind(workspace, Workspace)
      loop.bind(logger, LoggerService)
      loop.bind(serializer, new Serializer)
      loop.bind("commands", new Command)
      loop.addImports(
        "org.openmole.core.implementation.data._",
        "org.openmole.core.implementation.data.Prototype._",
        "org.openmole.core.implementation.data.Data._",
        "org.openmole.core.implementation.execution._",
        "org.openmole.core.implementation.execution.local._",
        "org.openmole.core.implementation.hook._",
        "org.openmole.core.implementation.job._",
        "org.openmole.core.implementation.mole._",
        "org.openmole.core.implementation.sampling._",
        "org.openmole.core.implementation.task._",
        "org.openmole.core.implementation.transition._",
        "org.openmole.core.implementation.tools._",
        "org.openmole.core.implementation.puzzle._",
        "org.openmole.misc.workspace._",
        "org.openmole.ui.console.Implicits._",
        "java.io.File",
        "commands._"
      )
    }
    loop.loop
  }
  
  //def setVariable(name: String, value: Object) = binding.setVariable(name, value)

  //def run(command: String) = groovysh.run(command)

  //def leftShift(cmnd: Command): Object = groovysh.leftShift(cmnd)
 
}
