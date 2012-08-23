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

package org.openmole.ui

import java.awt.GraphicsEnvironment
import java.awt.SplashScreen
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.Semaphore
import org.apache.clerezza.scala.console.Interpreter
import org.eclipse.equinox.app.IApplication
import org.eclipse.equinox.app.IApplicationContext
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import org.openmole.core.implementation.task.PluginSet
import org.openmole.ide.core.implementation.dialog.GUIApplication
import org.openmole.ui.console.Console
import scala.actors.threadpool.locks.ReentrantLock
import scala.swing.SimpleSwingApplication
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.ILoop
import scala.tools.nsc.interpreter.JLineCompletion
import scala.tools.nsc.interpreter.JLineReader
import scala.tools.nsc.interpreter.NoCompletion
import scala.tools.nsc.interpreter.ProductCompletion
import scala.tools.nsc.interpreter.SeqCompletion
import scopt.generic.OptionDefinition
import scopt.immutable._

class Application extends IApplication with Logger {
  override def start(context: IApplicationContext) = {

    case class Config(
      pluginsDirs: List[String] = Nil,
      guiPluginsDirs: List[String] = Nil,
      userPlugins: List[String] = Nil,
      workspaceDir: Option[String] = None)

    val parser = new OptionParser[Config]("openmole", "0.x") {
      def options = Seq(
        opt("cp", "pluginDirectories", "Plugins directories (seperated by \" \")") {
          (v: String, c: Config) ⇒ c.copy(pluginsDirs = v.split(' ').toList)
        },
        opt("gp", "guiPluginDirectories", "GUI plugins directories (seperated by \" \")") {
          (v: String, c: Config) ⇒ c.copy(guiPluginsDirs = v.split(' ').toList)
        },
        opt("p", "userPlugins", "Plugins (seperated by \" \")") {
          (v: String, c: Config) ⇒ c.copy(userPlugins = v.split(' ').toList)
        })
    }

    val args: Array[String] = context.getArguments.get("application.args").asInstanceOf[Array[String]]

    val console = args.contains("-c")
    val filtredArgs = args.filterNot((_: String) == "-c")

    parser.parse(filtredArgs, Config()) foreach { config ⇒

      /*val workspaceLocation = config.workspaceDir match {
        case Some(w) ⇒ new File(w)
        case None ⇒ Workspace.defaultLocation
      }*/

      //if (config.workspaceDir.isDefined) Workspace.instance = new Workspace(workspaceLocation)

      if (console) {
        try {
          val headless = GraphicsEnvironment.getLocalGraphicsEnvironment.isHeadlessInstance
          if (!headless && SplashScreen.getSplashScreen != null) SplashScreen.getSplashScreen.close
        } catch {
          case e ⇒ logger.log(FINE, "Error in splash screen closing", e)
        }
        /*if (Workspace.anotherIsRunningAt(workspaceLocation))
          logger.severe("Application is already runnig at " + workspaceLocation.getAbsolutePath + ". If it is not the case please remove the file '" + new File(workspaceLocation, Workspace.running).getAbsolutePath() + "'.")
        else {*/

        config.pluginsDirs.foreach { PluginManager.loadDir }

        val userPlugins = config.userPlugins.map { new File(_) }.toSet
        PluginManager.load(userPlugins)

        val console = new Console(new PluginSet(userPlugins))
        console.run
        // }
      } else {

        config.pluginsDirs.foreach { PluginManager.loadDir }
        config.guiPluginsDirs.foreach { PluginManager.loadDir }

        val waitClose = new Semaphore(0)
        val application = new GUIApplication() {
          override def closeOperation = {
            super.closeOperation
            waitClose.release(1)
          }
        }

        application.display
        waitClose.acquire(1)
      }

    }
    IApplication.EXIT_OK
  }

  override def stop = {}
}
