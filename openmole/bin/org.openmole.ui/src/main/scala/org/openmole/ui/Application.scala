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
import com.fasterxml.jackson.core.json.ByteSourceJsonBootstrapper
import org.eclipse.equinox.app.IApplication
import org.eclipse.equinox.app.IApplicationContext
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.logging.LoggerService
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.tools.service.Logger
import org.openmole.core.workspace.Workspace
import org.openmole.gui.bootstrap.js.BootstrapJS
import org.openmole.core.workflow.task._
import org.openmole.console.Console
import org.openmole.rest.server.RESTServer
import annotation.tailrec
import org.openmole.gui.server.core._

object Application extends Logger

import Application.Log._

class Application extends IApplication {

  lazy val consoleSplash =
    """  ___                   __  __  ___  _     _____   _  _
 / _ \ _ __   ___ _ __ |  \/  |/ _ \| |   | ____| | || |
| | | | '_ \ / _ \ '_ \| |\/| | | | | |   |  _|   | || |_
| |_| | |_) |  __/ | | | |  | | |_| | |___| |___  |__   _|
 \___/| .__/ \___|_| |_|_|  |_|\___/|_____|_____|    |_|
      |_|
"""

  override def start(context: IApplicationContext) = {

    sealed trait LaunchMode
    object ConsoleMode extends LaunchMode
    object GUIMode extends LaunchMode
    object HelpMode extends LaunchMode
    object ServerMode extends LaunchMode
    object ServerConfigMode extends LaunchMode

    case class Config(
      pluginsDirs: List[String] = Nil,
      guiPluginsDirs: List[String] = Nil,
      userPlugins: List[String] = Nil,
      workspaceDir: Option[String] = None,
      scriptFile: List[String] = Nil,
      password: Option[String] = None,
      hostName: Option[String] = None,
      launchMode: LaunchMode = GUIMode,
      ignored: List[String] = Nil,
      allowInsecureConnections: Boolean = false,
      serverPort: Option[Int] = None,
      serverSSLPort: Option[Int] = None,
      loggerLevel: Option[String] = None,
      optimizedJS: Boolean = false)

    def takeArg(args: List[String]) =
      args match {
        case h :: t ⇒ h
        case Nil    ⇒ ""
      }

    def dropArg(args: List[String]) =
      args match {
        case h :: t ⇒ t
        case Nil    ⇒ Nil
      }

    def takeArgs(args: List[String]) = args.takeWhile(!_.startsWith("-"))
    def dropArgs(args: List[String]) = args.dropWhile(!_.startsWith("-"))

    def usage =
      """openmole [options]

[-p list of arg] plugins list of jar or category containing jars to be loaded
[-s path] a path of script to execute
[-pw password] openmole password
[-c] console mode
[-h] print help"""

    @tailrec def parse(args: List[String], c: Config = Config()): Config =
      args match {
        case "-cp" :: tail                          ⇒ parse(dropArgs(tail), c.copy(pluginsDirs = takeArgs(tail)))
        case "-gp" :: tail                          ⇒ parse(dropArgs(tail), c.copy(guiPluginsDirs = takeArgs(tail)))
        case "-p" :: tail                           ⇒ parse(dropArgs(tail), c.copy(userPlugins = takeArgs(tail)))
        case "-s" :: tail                           ⇒ parse(dropArgs(tail), c.copy(scriptFile = takeArgs(tail)))
        case "-pw" :: tail                          ⇒ parse(dropArg(tail), c.copy(password = Some(takeArg(tail))))
        case "-hn" :: tail                          ⇒ parse(tail.tail, c.copy(hostName = Some(tail.head)))
        case "-c" :: tail                           ⇒ parse(tail, c.copy(launchMode = ConsoleMode))
        case "-h" :: tail                           ⇒ parse(tail, c.copy(launchMode = HelpMode))
        case "-ws" :: tail                          ⇒ parse(tail, c.copy(launchMode = ServerMode))
        case "--ws-configure" :: tail               ⇒ parse(tail, c.copy(launchMode = ServerConfigMode))
        case "-sp" :: tail                          ⇒ parse(tail.tail, c.copy(serverPort = Some(tail.head.toInt))) // Server port
        case "-ssp" :: tail                         ⇒ parse(tail.tail, c.copy(serverSSLPort = Some(tail.head.toInt)))
        case "--allow-insecure-connections" :: tail ⇒ parse(tail, c.copy(allowInsecureConnections = true))
        case "--logger-level" :: tail               ⇒ parse(tail.tail, c.copy(loggerLevel = Some(tail.head)))
        case "--optimizedJS" :: tail                ⇒ parse(tail, c.copy(optimizedJS = true))
        case s :: tail                              ⇒ parse(tail, c.copy(ignored = s :: c.ignored))
        case Nil                                    ⇒ c
      }

    val args: Array[String] = context.getArguments.get("application.args").asInstanceOf[Array[String]].map(_.trim)

    val config = parse(args.toList)

    config.loggerLevel.foreach(LoggerService.level)

    val (existingUserPlugins, notExistingUserPlugins) = config.userPlugins.span(new File(_).exists)

    if (!notExistingUserPlugins.isEmpty) logger.warning(s"""Some plugins or plugin folders don't exist: ${notExistingUserPlugins.mkString(",")}""")

    val userPlugins =
      existingUserPlugins.flatMap { p ⇒ PluginManager.plugins(new File(p)) }

    logger.fine(s"Loading user plugins " + userPlugins)

    val plugins: List[String] =
      config.pluginsDirs ++
        existingUserPlugins ++
        (if (config.launchMode == GUIMode) config.guiPluginsDirs else List.empty)

    val bundles = PluginManager.load(plugins.map(new File(_)))
    PluginManager.startAll

    try config.password foreach Workspace.setPassword
    catch {
      case e: UserBadDataError ⇒
        logger.severe("Wrong password!")
        throw e
    }

    if (!config.ignored.isEmpty) logger.warning("Ignored options: " + config.ignored.mkString(" "))

    config.launchMode match {
      case HelpMode ⇒ println(usage)
      case ServerConfigMode ⇒
        RESTServer.configure
      case ServerMode ⇒
        if (!config.password.isDefined) Console.initPassword
        val server = new RESTServer(config.serverPort, config.serverSSLPort, config.hostName, config.allowInsecureConnections, PluginSet(userPlugins))
        server.start()
      case ConsoleMode ⇒
        print(consoleSplash)
        val console = new Console(PluginSet(userPlugins), config.password, config.scriptFile)
        console.run
      case GUIMode ⇒
        BootstrapJS.init(config.optimizedJS)
        val server = new GUIServer(config.serverPort, BootstrapJS.webapp)
        server.start()
    }

    IApplication.EXIT_OK
  }

  override def stop = {}
}
