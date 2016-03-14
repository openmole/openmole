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

import java.awt.Desktop
import java.io.{ FileOutputStream, File }
import java.net.URI
import org.eclipse.equinox.app.IApplication
import org.eclipse.equinox.app.IApplicationContext
import org.openmole.core.project._
import org.openmole.core.console.ScalaREPL
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.logging.LoggerService
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.replication.DBServerRunning
import org.openmole.core.tools.io.Network
import org.openmole.core.workspace.Workspace
import org.openmole.core.workflow.task._
import org.openmole.rest.server.RESTServer
import org.openmole.tool.logger.Logger
import annotation.tailrec
import org.openmole.gui.server.core._
import org.openmole.runtime.console._
import org.openmole.tool.file._

object Application extends Logger

import Application.Log._

class Application extends IApplication {

  lazy val consoleSplash =
    """  ___                   __  __  ___  _     _____   ____
 / _ \ _ __   ___ _ __ |  \/  |/ _ \| |   | ____| | ___|
      | | | | '_ \ / _ \ '_ \| |\/| | | | | |   |  _|   |___ \
      | |_| | |_) |  __/ | | | |  | | |_| | |___| |___   ___) |
 \___/| .__/ \___|_| |_|_|  |_|\___/|_____|_____| |____/
      |_|
    """

  lazy val consoleUsage = "(Type :q to quit)"

  override def start(context: IApplicationContext) = DBServerRunning.useDB {

    sealed trait LaunchMode
    object ConsoleMode extends LaunchMode
    object GUIMode extends LaunchMode
    object HelpMode extends LaunchMode
    object ServerMode extends LaunchMode
    object ServerConfigMode extends LaunchMode

    case class Config(
      pluginsDirs:          List[String]    = Nil,
      guiPluginsDirs:       List[String]    = Nil,
      userPlugins:          List[String]    = Nil,
      loadHomePlugins:      Option[Boolean] = None,
      workspaceDir:         Option[String]  = None,
      scriptFile:           Option[String]  = None,
      consoleWorkDirectory: Option[File]    = None,
      password:             Option[String]  = None,
      hostName:             Option[String]  = None,
      launchMode:           LaunchMode      = GUIMode,
      ignored:              List[String]    = Nil,
      port:                 Option[Int]     = None,
      loggerLevel:          Option[String]  = None,
      unoptimizedJS:        Boolean         = false,
      remote:               Boolean         = false,
      browse:               Boolean         = true,
      reset:                Boolean         = false,
      args:                 List[String]    = Nil
    )

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
        case "-cp" :: tail                     ⇒ parse(dropArgs(tail), c.copy(pluginsDirs = takeArgs(tail)))
        case "-gp" :: tail                     ⇒ parse(dropArgs(tail), c.copy(guiPluginsDirs = takeArgs(tail)))
        case "-p" :: tail                      ⇒ parse(dropArgs(tail), c.copy(userPlugins = takeArgs(tail)))
        case "-s" :: tail                      ⇒ parse(dropArg(tail), c.copy(scriptFile = Some(takeArg(tail)), launchMode = ConsoleMode))
        case "-pw" :: tail                     ⇒ parse(dropArg(tail), c.copy(password = Some(takeArg(tail))))
        case "-hn" :: tail                     ⇒ parse(tail.tail, c.copy(hostName = Some(tail.head)))
        case "-c" :: tail                      ⇒ parse(tail, c.copy(launchMode = ConsoleMode))
        case "-h" :: tail                      ⇒ parse(tail, c.copy(launchMode = HelpMode))
        case "-ws" :: tail                     ⇒ parse(tail, c.copy(launchMode = ServerMode))
        case "--load-homePlugins" :: tail      ⇒ parse(tail, c.copy(loadHomePlugins = Some(true)))
        case "--console-workDirectory" :: tail ⇒ parse(dropArg(tail), c.copy(consoleWorkDirectory = Some(new File(takeArg(tail)))))
        case "--ws-configure" :: tail          ⇒ parse(tail, c.copy(launchMode = ServerConfigMode))
        case "--port" :: tail                  ⇒ parse(tail.tail, c.copy(port = Some(tail.head.toInt))) // Server port
        case "--logger-level" :: tail          ⇒ parse(tail.tail, c.copy(loggerLevel = Some(tail.head)))
        case "--unoptimizedJS" :: tail         ⇒ parse(tail, c.copy(unoptimizedJS = true))
        case "--webui-authentication" :: tail  ⇒ parse(tail, c.copy(remote = true))
        case "--remote" :: tail                ⇒ parse(tail, c.copy(remote = true))
        case "--no-browser" :: tail            ⇒ parse(tail, c.copy(browse = false))
        case "--reset" :: tail                 ⇒ parse(tail, c.copy(reset = true))
        case "--" :: tail                      ⇒ parse(Nil, c.copy(args = tail))
        case s :: tail                         ⇒ parse(tail, c.copy(ignored = s :: c.ignored))
        case Nil                               ⇒ c
      }

    val args: Array[String] = context.getArguments.get("application.args").asInstanceOf[Array[String]].map(_.trim)

    val config = parse(args.toList)

    config.loggerLevel.foreach(LoggerService.level)

    if (config.reset) {
      Workspace.reset()
      new Integer(Console.ExitCodes.ok)
    }
    else {
      val (existingUserPlugins, notExistingUserPlugins) = config.userPlugins.span(new File(_).exists)

      if (!notExistingUserPlugins.isEmpty) logger.warning(s"""Some plugins or plugin folders don't exist: ${notExistingUserPlugins.mkString(",")}""")

      val userPlugins =
        existingUserPlugins.flatMap { p ⇒ PluginManager.plugins(new File(p)) } ++
          (if (config.loadHomePlugins.getOrElse(config.launchMode != ConsoleMode)) Workspace.pluginDir.listFilesSafe.flatMap(PluginManager.plugins) else Nil)

      logger.fine(s"Loading user plugins " + userPlugins)

      val plugins: List[File] =
        config.pluginsDirs.map(new File(_)) ++
          userPlugins ++
          (if (config.launchMode == GUIMode) config.guiPluginsDirs.map(new File(_)) else List.empty)

      PluginManager.startAll.foreach { case (b, e) ⇒ logger.log(WARNING, s"Error staring bundle $b", e) }
      PluginManager.tryLoad(plugins).foreach { case (b, e) ⇒ logger.log(WARNING, s"Error loading bundle $b", e) }

      try config.password foreach Workspace.setPassword
      catch {
        case e: UserBadDataError ⇒
          logger.severe("Wrong password!")
          throw e
      }

    if (!config.ignored.isEmpty) logger.warning("Ignored options: " + config.ignored.mkString(" "))

    val retCode: Int =
      config.launchMode match {
        case HelpMode ⇒
          println(usage)
          Console.ExitCodes.ok
        case ServerConfigMode ⇒
          RESTServer.configure
          Console.ExitCodes.ok
        case ServerMode ⇒
          if (!config.password.isDefined) Console.initPassword
          val server = new RESTServer(config.port, config.hostName)
          server.start()
          Console.ExitCodes.ok
        case ConsoleMode ⇒
          print(consoleSplash)
          println(consoleUsage)
          val console = new Console(config.password, config.scriptFile)
          val variables = ConsoleVariables(args = config.args)
          console.run(variables, config.consoleWorkDirectory)
        case GUIMode ⇒
          def browse(url: String) =
            if (Desktop.isDesktopSupported) Desktop.getDesktop.browse(new URI(url))
          GUIServer.lockFile.withFileOutputStream { fos ⇒
            val launch = (config.remote || fos.getChannel.tryLock != null)
            if (launch) {
              val port = config.port.getOrElse(Workspace.preferenceAsInt(GUIServer.port))
              val url = s"https://localhost:$port"
              GUIServer.urlFile.content = url
              if (config.remote) GUIServer.initPassword
              //The webapp location will then be somewhere in target
              val webui = Workspace.file("webui")
              webui.mkdirs()
              val server = new GUIServer(port, config.remote)
              server.start()
              browse(url)
              ScalaREPL.warmup
              logger.info(s"Server listening on port $port.")
              server.join()
            }
            else {
              browse(GUIServer.urlFile.content)
              Console.ExitCodes.ok
            }
        }

      new Integer(retCode)
    }

  }

  override def stop = {}
}
