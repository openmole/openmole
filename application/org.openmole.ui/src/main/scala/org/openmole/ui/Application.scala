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
import annotation.tailrec

class Application extends IApplication with Logger {
  override def start(context: IApplicationContext) = {

    case class Config(
      pluginsDirs: List[String] = Nil,
      guiPluginsDirs: List[String] = Nil,
      userPlugins: List[String] = Nil,
      workspaceDir: Option[String] = None,
      scriptFile: Option[String] = None,
      password: Option[String] = None,
      console: Boolean = false)

    def takeArgs(args: List[String]) = args.takeWhile(_.startsWith("-"))
    def dropArgs(args: List[String]) = args.dropWhile(!_.startsWith("-"))

    @tailrec def parse(args: List[String], c: Config = Config()): Config =
      args match {
        case "-cp" :: tail ⇒ parse(dropArgs(tail), c.copy(pluginsDirs = takeArgs(tail)))
        case "-gp" :: tail ⇒ parse(dropArgs(tail), c.copy(guiPluginsDirs = takeArgs(tail)))
        case "-p" :: tail ⇒ parse(dropArgs(tail), c.copy(userPlugins = takeArgs(tail)))
        case "-s" :: tail ⇒ parse(tail.tail, c.copy(scriptFile = Some(tail.head)))
        case "-pw" :: tail ⇒ parse(tail.tail, c.copy(password = Some(tail.head)))
        case "-c" :: tail ⇒ parse(tail, c.copy(console = true))
        case Nil ⇒ c
        case s :: tail ⇒ println("Ignored arg " + s); c
      }

    val args: Array[String] = context.getArguments.get("application.args").asInstanceOf[Array[String]]

    val config = parse(args.toList)

    config.pluginsDirs.foreach { PluginManager.load }

    val userPlugins = config.userPlugins.map { new File(_) }.toSet
    PluginManager.load(userPlugins)

    if (config.console) {
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
    IApplication.EXIT_OK
  }

  override def stop = {}
}
