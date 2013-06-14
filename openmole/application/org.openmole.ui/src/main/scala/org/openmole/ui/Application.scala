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
import annotation.tailrec
import org.openmole.web._
import org.openmole.misc.exception.UserBadDataError

class Application extends IApplication with Logger {
  override def start(context: IApplicationContext) = {

    case class Config(
      pluginsDirs: List[String] = Nil,
      guiPluginsDirs: List[String] = Nil,
      userPlugins: List[String] = Nil,
      workspaceDir: Option[String] = None,
      scriptFile: Option[String] = None,
      password: Option[String] = None,
      console: Boolean = false,
      help: Boolean = false,
      ignored: List[String] = Nil,
      server: Boolean = false,
      serverPort: Option[Int] = None)

    def takeArgs(args: List[String]) = args.takeWhile(!_.startsWith("-"))
    def dropArgs(args: List[String]) = args.dropWhile(!_.startsWith("-"))

    def usage =
      """openmole [options]

[-p list of arg] plugins list of jar or dir containing jars to be loaded
[-s path] a path of script to execute
[-pw password] openmole password
[-c] console mode
[-h] print help"""

    @tailrec def parse(args: List[String], c: Config = Config()): Config =
      args match {
        case "-cp" :: tail ⇒ parse(dropArgs(tail), c.copy(pluginsDirs = takeArgs(tail)))
        case "-gp" :: tail ⇒ parse(dropArgs(tail), c.copy(guiPluginsDirs = takeArgs(tail)))
        case "-p" :: tail  ⇒ parse(dropArgs(tail), c.copy(userPlugins = takeArgs(tail)))
        case "-s" :: tail  ⇒ parse(tail.tail, c.copy(scriptFile = Some(tail.head)))
        case "-pw" :: tail ⇒ parse(tail.tail, c.copy(password = Some(tail.head)))
        case "-c" :: tail  ⇒ parse(tail, c.copy(console = true))
        case "-h" :: tail  ⇒ parse(tail, c.copy(help = true))
        case "-ws" :: tail ⇒ parse(tail, c.copy(server = true))
        case "-sp" :: tail ⇒ parse(tail.tail, c.copy(serverPort = Some(tail.head.toInt)))
        case s :: tail     ⇒ parse(tail, c.copy(ignored = s :: c.ignored))
        case Nil           ⇒ c
      }

    val args: Array[String] = context.getArguments.get("application.args").asInstanceOf[Array[String]]

    val config = parse(args.toList)

    val userPlugins = config.userPlugins.map(p ⇒ new File(p))

    val plugins: List[String] =
      config.pluginsDirs ++
        config.userPlugins ++
        (if (!config.console && !config.server) config.guiPluginsDirs else List.empty)

    PluginManager.load(
      plugins.map(p ⇒ new File(p))
    )

    try {
      config.password foreach Workspace.setPassword
    }
    catch { case e: UserBadDataError ⇒ println("Wrong password!!"); System.exit(1) }

    if (!config.ignored.isEmpty) println("Ignored options: " + config.ignored.mkString(" "))

    if (config.help) println(usage)
    else if (config.console) {
      try {
        val headless = GraphicsEnvironment.getLocalGraphicsEnvironment.isHeadlessInstance
        if (!headless && SplashScreen.getSplashScreen != null) SplashScreen.getSplashScreen.close
      }
      catch {
        case e: Throwable ⇒ logger.log(FINE, "Error in splash screen closing", e)
      }

      val console = new Console(PluginSet(userPlugins), config.password, config.scriptFile)
      console.run
    }
    else if (config.server) {
      try {
        if (SplashScreen.getSplashScreen != null) SplashScreen.getSplashScreen.close
      }
      catch {
        case e: Throwable ⇒ logger.log(FINE, "Error in splash screen closing", e)
      }

      val server = new Openmolewebserver(config.serverPort getOrElse 80)

      server.start()
    }
    else {

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
