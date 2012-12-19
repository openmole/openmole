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
import org.eclipse.equinox.app.IApplication
import org.eclipse.equinox.app.IApplicationContext
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.dialog.GUIApplication
import org.openmole.ui.console.Console
import scopt.generic.OptionDefinition
import scopt.immutable._

class Application extends IApplication with Logger {
  override def start(context: IApplicationContext) = {

    case class Config(
      pluginsDirs: List[String] = Nil,
      guiPluginsDirs: List[String] = Nil,
      userPlugins: List[String] = Nil,
      workspaceDir: Option[String] = None,
      scriptFile: Option[String] = None,
      password: Option[String] = None)

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
        },
        opt("s", "script", "Script file to execute") {
          (v: String, c: Config) ⇒ c.copy(scriptFile = Some(v))
        },
        opt("pw", "password", "Password for the preferences encryption") {
          (v: String, c: Config) ⇒ c.copy(password = Some(v))
        })
    }

    val args: Array[String] = context.getArguments.get("application.args").asInstanceOf[Array[String]]

    val console = args.contains("-c")
    val filtredArgs = args.filterNot((_: String) == "-c")

    parser.parse(filtredArgs, Config()) foreach { config ⇒

      config.pluginsDirs.foreach { PluginManager.load }

      val userPlugins = config.userPlugins.map { new File(_) }.toSet
      PluginManager.load(userPlugins)

      if (console) {
        try {
          val headless = GraphicsEnvironment.getLocalGraphicsEnvironment.isHeadlessInstance
          if (!headless && SplashScreen.getSplashScreen != null) SplashScreen.getSplashScreen.close
        } catch {
          case e: Throwable ⇒ logger.log(FINE, "Error in splash screen closing", e)
        }

        val console = new Console(PluginSet(userPlugins), config.password, config.scriptFile)
        console.run
      } else {

        config.guiPluginsDirs.foreach { PluginManager.load }

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
