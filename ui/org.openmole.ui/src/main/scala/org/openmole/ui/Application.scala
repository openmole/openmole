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

package org.openmole.ui

import java.io.File
import java.util.concurrent.Semaphore
import org.eclipse.equinox.app.IApplication
import org.eclipse.equinox.app.IApplicationContext
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import org.openmole.ui.console.Console
import org.openmole.ui.console.command.Encrypt
import org.openmole.ui.console.command.Get
import org.openmole.ui.console.command.Print
import org.openmole.ui.console.command.Auth
import org.openmole.ui.console.command.Verify
import org.openmole.ide.core.test.TestPanel
import scala.actors.threadpool.locks.ReentrantLock
import scala.swing.SimpleSwingApplication
import scopt.generic.OptionDefinition
import scopt.immutable._

class Application extends IApplication with Logger {
  override def start(context: IApplicationContext) = {
    
    
    case class Config(
      pluginsDirs: List[String] = Nil,
      workspaceDir: Option[String] = None
    )
    
    
    val parser = new OptionParser[Config]("openmole", "0.x") { 
      def options = Seq(
        opt("p", "pluginDirectories" ,"Plugins directories (seperated by \",\")") {
          (v: String, c: Config) => c.copy(pluginsDirs = v.split(',').toList) 
        },
        opt("w", "Path of the workspace") {
          (v: String, c: Config) => c.copy(workspaceDir = Some(v)) 
        }
      )
    }
      
    
    val args: Array[String] = context.getArguments.get("application.args").asInstanceOf[Array[String]]
    
    
    val gui = args.contains("-gui")
    val filtredArgs = args.filterNot((_: String) == "-gui")
    
    parser.parse(filtredArgs, Config()) foreach { config =>
    
      val workspaceLocation = config.workspaceDir match {
        case Some(w) => new File(w)
        case None => Workspace.defaultLocation
      }

      if(!gui) {
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
      } else {
        val waitClose = new Semaphore(0)
        val pannel = new TestPanel() {
          override def closeOperation = {
            super.closeOperation                     
            waitClose.release(1)
          }
        }
        pannel.visible = true
        waitClose.acquire(1)
      }

    } 
    IApplication.EXIT_OK
  }
  
  override def stop = {}
}
