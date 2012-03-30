/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.ui.console

import java.io.File
import org.eclipse.equinox.app.IApplication
import org.eclipse.equinox.app.IApplicationContext
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import org.openmole.ui.console.internal.command.Auth
import org.openmole.ui.console.internal.command.Encrypt
import org.openmole.ui.console.internal.command.Get
import org.openmole.ui.console.internal.command.Print
import org.openmole.ui.console.internal.command.Verify
import scopt.immutable._

class Application extends IApplication with Logger {
  override def start(context: IApplicationContext) = {
    
    case class Config(
      pluginsDirs: List[String] = Nil,
      workspaceDir: Option[String] = None
    )
    
    
    val parser = new OptionParser[Config]("openmole-console", "0.x") { 
      def options = Seq(
        opt("p", "pluginDirectories" ,"Plugins directories (seperated by \",\")") {
          (v: String, c: Config) => c.copy(pluginsDirs = v.split(',').toList) 
        },
        opt("w", "Path of the workspace") {
          (v: String, c: Config) => c.copy(workspaceDir = Some(v)) 
        }
      )
    }
      
    parser.parse(context.getArguments.get("application.args").asInstanceOf[Array[String]], Config()) foreach { config =>
    
      val workspaceLocation = config.workspaceDir match {
        case Some(w) => new File(w)
        case None => Workspace.defaultLocation
      }

      if(Workspace.anotherIsRunningAt(workspaceLocation)) 
        logger.severe("Application is already runnig at " + workspaceLocation.getAbsolutePath + ". If it is not the case please remove the file '" + new File(workspaceLocation, Workspace.running).getAbsolutePath() + "'.")  
      else {       
        if(config.workspaceDir.isDefined) Workspace.instance = new Workspace(workspaceLocation)
 
        val g = Console.groovysh
        g.leftShift(new Print(g, "print", "\\pr"))
        g.leftShift(new Get(g, "get", "\\g"))
        g.leftShift(new Auth(g, "auth", "\\au"))
        g.leftShift(new Encrypt(g, "encrypt", "\\en"))
        g.leftShift(new Verify(g, "verify", "\\vf"))
         
        config.pluginsDirs.foreach { PluginManager.loadDir }
        
        // Run
        Console.initPassword
        Console.groovysh.run()
      } 
    } 
    IApplication.EXIT_OK
  }
  
  override def stop = {}
}
