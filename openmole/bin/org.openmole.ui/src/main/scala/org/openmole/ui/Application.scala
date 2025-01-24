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
import java.io.{File, FileOutputStream, IOException}
import java.util.logging.Level
import java.net.URI
import org.openmole.console.Console.ExitCodes
import org.openmole.core.project.*
import org.openmole.core.logconfig.LoggerConfig
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.rest.server.RESTServer
import org.openmole.tool.logger.JavaLogger

import annotation.tailrec
import org.openmole.gui.server.core.*
import org.openmole.console.*
import org.openmole.core.authentication.AuthenticationStore
import org.openmole.tool.file.*
import org.openmole.tool.hash.*
import org.openmole.core.{location, module}
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.preference.*
import org.openmole.core.services.*
import org.openmole.core.networkservice.*
import org.openmole.tool.outputredirection.OutputRedirection

object Application extends JavaLogger {

  import Log._

  def run(args: Array[String]): Int = {

    sealed trait LaunchMode
    object ConsoleMode extends LaunchMode
    object GUIMode extends LaunchMode
    object HelpMode extends LaunchMode
    object RESTMode extends LaunchMode
    object VersionMode extends LaunchMode
    case class Reset(initialisePassword: Boolean) extends LaunchMode
    case class TestCompile(files: List[File]) extends LaunchMode

    case class Config(
      userPlugins:              List[String]    = Nil,
      loadHomePlugins:          Option[Boolean] = None,
      scriptFile:               Option[String]  = None,
      consoleWorkDirectory:     Option[File]    = None,
      password:                 Option[String]  = None,
      passwordFile:             Option[File]    = None,
      workspace:                Option[File]    = None,
      launchMode:               LaunchMode      = GUIMode,
      ignored:                  List[String]    = Nil,
      port:                     Option[Int]     = None,
      loggerLevel:              Option[String]  = None,
      loggerFileLevel:          Option[String]  = None,
      unoptimizedJS:            Boolean         = false,
      remote:                   Boolean         = false,
      browse:                   Boolean         = true,
      proxyURI:                 Option[String]  = None,
      debugNoOutputRedirection: Boolean         = false,
      args:                     List[String]    = Nil,
      guiExtraHeader:           Option[String]  = None,
      guiExtraHeaderFile:       Option[File]    = None
    )

    def takeArg(args: List[String]) =
      args match
        case h :: t ⇒ h
        case Nil    ⇒ ""

    def dropArg(args: List[String]) =
      args match
        case h :: t ⇒ t
        case Nil    ⇒ Nil

    def takeArgs(args: List[String]) = args.takeWhile(!_.startsWith("-"))
    def dropArgs(args: List[String]) = args.dropWhile(!_.startsWith("-"))

    def usage =
      """OpenMOLE application options:
      |[-p | --plugin list of arg] plugins list of jar or category containing jars to be loaded
      |[-c | --console] console mode
      |[--port port] specify the port for the GUI or REST API
      |[--script path] a path of an OpenMOLE script to execute
      |[--password password] openmole password
      |[--password-file file containing a password] read the OpenMOLE password (--password option) in a file
      |[--workspace directory] run openmole with an alternative workspace location
      |[--rest] run the REST server
      |[--remote] enable remote connection to the web interface
      |[--no-browser] don't automatically launch the browser in GUI mode
      |[--unoptimized-js] do not optimize JS (do not use Google Closure Compiler)
      |[--load-workspace-plugins] load the plugins of the OpenMOLE workspace (these plugins are always loaded in GUI mode)
      |[--console-work-directory] specify the workDirectory variable in console mode (it is set to the current directory by default)
      |[--reset] reset all preferences and authentications
      |[--reset-password] reset all preferences and ask for the a password
      |[--mem memory] allocate more memory to the JVM (not supported on windows yes), for instance --mem 2G
      |[--logger-level level] set the level of logging (OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL)
      |[--logger-file-level level] set the level of logging if log file (OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL)
      |[--proxy hostname] set the proxy in the form http://myproxy.org:3128
      |[--gui-extra-header ] specify a piece of html code to be inserted in the GUI html header file
      |[--gui-extra-header-file] specify a file containing a piece of html code to be inserted in the GUI html header file
      |[--gui-base-path] specify base path for GUI, can be used when OpenMOLE runs behind a reverse proxy
      |[--debug-no-output-redirection] deactivate system output redirection
      |[--] end of options the remaining arguments are provided to the console in the args array
      |[-h | --help] print help
      |[--version] print version information""".stripMargin

    def parse(args: List[String], c: Config = Config()): Config =
      def plugins(tail: List[String]) = parse(dropArgs(tail), c.copy(userPlugins = takeArgs(tail)))
      def help(tail: List[String]) = parse(tail, c.copy(launchMode = HelpMode))
      def script(tail: List[String]) = parse(dropArg(tail), c.copy(scriptFile = Some(takeArg(tail)), launchMode = ConsoleMode))
      def console(tail: List[String]) = parse(tail, c.copy(launchMode = ConsoleMode))
      args match
        case "-p" :: tail                            ⇒ plugins(tail)
        case "--plugins" :: tail                     ⇒ plugins(tail)
        case "-c" :: tail                            ⇒ console(tail)
        case "--console" :: tail                     ⇒ console(tail)
        case "-s" :: tail                            ⇒ script(tail)
        case "--script" :: tail                      ⇒ script(tail)
        case "--port" :: tail                        ⇒ parse(tail.tail, c.copy(port = Some(tail.head.toInt)))
        case "--password" :: tail                    ⇒ parse(dropArg(tail), c.copy(password = Some(takeArg(tail))))
        case "--password-file" :: tail               ⇒ parse(dropArg(tail), c.copy(passwordFile = Some(new File(takeArg(tail)))))
        case "--workspace" :: tail                   ⇒ parse(dropArg(tail), c.copy(workspace = Some(new File(takeArg(tail)))))
        case "--rest" :: tail                        ⇒ parse(tail, c.copy(launchMode = RESTMode))
        case "--load-workspace-plugins" :: tail      ⇒ parse(tail, c.copy(loadHomePlugins = Some(true)))
        case "--console-work-directory" :: tail      ⇒ parse(dropArg(tail), c.copy(consoleWorkDirectory = Some(new File(takeArg(tail)))))
        case "--logger-level" :: tail                ⇒ parse(tail.tail, c.copy(loggerLevel = Some(tail.head)))
        case "--logger-file-level" :: tail           ⇒ parse(tail.tail, c.copy(loggerFileLevel = Some(tail.head)))
        case "--remote" :: tail                      ⇒ parse(tail, c.copy(remote = true))
        case "--no-browser" :: tail                  ⇒ parse(tail, c.copy(browse = false))
        case "--unoptimizedJS" :: tail               ⇒ parse(tail, c.copy(unoptimizedJS = true))
        case "--unoptimized-js" :: tail              ⇒ parse(tail, c.copy(unoptimizedJS = true))
        case "--gui-extra-header" :: tail            ⇒ parse(dropArg(tail), c.copy(guiExtraHeader = Some(takeArg(tail))))
        case "--gui-extra-header-file" :: tail       ⇒ parse(dropArg(tail), c.copy(guiExtraHeaderFile = Some(new File(takeArg(tail)))))
        case "--reset" :: tail                       ⇒ parse(tail, c.copy(launchMode = Reset(initialisePassword = false)))
        case "--reset-password" :: tail              ⇒ parse(tail, c.copy(launchMode = Reset(initialisePassword = true)))
        case "--proxy" :: tail                       ⇒ parse(tail.tail, c.copy(proxyURI = Some(tail.head)))
        case "--debug-no-output-redirection" :: tail ⇒ parse(tail, c.copy(debugNoOutputRedirection = true))
        case "--" :: tail                            ⇒ parse(Nil, c.copy(args = tail))
        case "-h" :: tail                            ⇒ help(tail)
        case "--help" :: tail                        ⇒ help(tail)
        case "--version" :: tail                     ⇒ parse(tail, c.copy(launchMode = VersionMode))
        case "--test-compile" :: tail                ⇒ parse(dropArgs(tail), c.copy(launchMode = TestCompile(takeArgs(tail).map(p ⇒ new File(p)))))
        case s :: tail                               ⇒ parse(tail, c.copy(ignored = s :: c.ignored))
        case Nil                                     ⇒ c

    PluginManager.startAll.foreach { case (b, e) ⇒ logger.log(WARNING, s"Error staring bundle $b", e) }

    val config = parse(args.toVector.map(_.trim).toList)

    val logLevel = config.loggerLevel.map(l ⇒ Level.parse(l.toUpperCase))
    logLevel.foreach(LoggerConfig.level)
    val logFileLevel = config.loggerFileLevel.map(l ⇒ Level.parse(l.toUpperCase))

    if (config.debugNoOutputRedirection) OutputManager.uninstall

    val workspaceDirectory = config.workspace.getOrElse(org.openmole.core.workspace.defaultOpenMOLEDirectory)

    implicit val workspace: Workspace = Workspace(workspaceDirectory)
    import org.openmole.tool.thread._

    //Runtime.getRuntime.addShutdownHook(thread(Workspace.clean(workspace)))

    def loadPlugins(implicit workspace: Workspace) =
      val (existingUserPlugins, notExistingUserPlugins) = config.userPlugins.span(new File(_).exists)

      if (!notExistingUserPlugins.isEmpty) logger.warning(s"""Some plugins or plugin folders don't exist: ${notExistingUserPlugins.mkString(",")}""")

      val userPlugins =
        existingUserPlugins.flatMap { p ⇒ PluginManager.listBundles(new File(p)) } ++ module.allModules

      logger.fine(s"Loading user plugins " + userPlugins)

      PluginManager.tryLoad(userPlugins)

    def displayErrors(load: ⇒ Iterable[(File, Throwable)]) =
      load.foreach { case (f, e) ⇒ logger.log(WARNING, s"Error loading bundle $f", e) }

    def password = config.password orElse config.passwordFile.map(_.lines.head)

    if (!config.ignored.isEmpty) logger.warning("Ignored options: " + config.ignored.reverse.mkString(" "))

    config.launchMode match
      case VersionMode ⇒
        println(
          s"""OpenMOLE version: ${org.openmole.core.buildinfo.version.value} - ${org.openmole.core.buildinfo.name}
          |Built: ${org.openmole.core.buildinfo.version.generationDate} at ${org.openmole.core.buildinfo.version.generationTime}""".stripMargin)
        Console.ExitCodes.ok
      case HelpMode ⇒
        println(usage)
        Console.ExitCodes.ok
      case Reset(initialisePassword) ⇒
        Console.withTerminal:
          given Preference = Services.preference(workspace)
          given AuthenticationStore = Services.authenticationStore(workspace)
          Services.resetPassword
          if initialisePassword then Console.initPassword
          Console.ExitCodes.ok
      case RESTMode ⇒
        given Preference = Services.preference(workspace)
        displayErrors(loadPlugins)

        Services.withServices(workspaceDirectory, config.password, config.proxyURI, logLevel, logFileLevel): services ⇒
          Runtime.getRuntime.addShutdownHook(thread(Services.dispose(services)))
          val server = new RESTServer(config.port, !config.remote, services)
          server.run()

        Console.ExitCodes.ok
      case ConsoleMode ⇒
        Console.withTerminal:
          given Preference = Services.preference(workspace)
          
          Console.dealWithLoadError(loadPlugins, !config.scriptFile.isDefined)
          Services.withServices(workspaceDirectory, config.password, config.proxyURI, logLevel, logFileLevel) { implicit services ⇒
            Runtime.getRuntime.addShutdownHook(thread(Services.dispose(services)))
            val console = new Console(config.scriptFile)
            console.run(config.args, config.consoleWorkDirectory)
          }

      case GUIMode ⇒
        given preference: Preference = Services.preference(workspace)

        // FIXME switch to a GUI display in the plugin panel
        displayErrors(loadPlugins)

        def browse(url: String) =
          if Desktop.isDesktopSupported
          then
            try Desktop.getDesktop.browse(new URI(url))
            catch
              case t: Throwable => logger.warning("Unable to open OpenMOLE app page in the browser")

        GUIServer.lockFile.withFileOutputStream: fos ⇒
          val launch = config.remote || fos.getChannel.tryLock != null
          if launch
          then
            GUIServer.initialisePreference(preference)
            val port = config.port.getOrElse(preference(GUIServer.port))
            val extraHeader =
              config.guiExtraHeader.getOrElse("") +
              config.guiExtraHeaderFile.map { _.content }.getOrElse("")

            val url =  s"http://localhost:$port"

            GUIServer.urlFile.content = url
            
            GUIServerServices.withServices(workspace, config.proxyURI, logLevel, logFileLevel, config.password): services ⇒
              val newServer = GUIServer(port, !config.remote, services, config.password, !config.unoptimizedJS, extraHeader)

              val s = newServer.start()
              registerSignalCatcher: si =>
                logger.info(s"Received signal $si, shutting down")
                s.stop()

              if config.browse && !config.remote then browse(url)

              logger.info(
                "\n" + org.openmole.core.buildinfo.consoleSplash + "\n" +
                  s"Server listening on port $port."
              )

              // warmup the scala compiler
              def warmup() =
                import services.*
                org.openmole.core.project.OpenMOLEREPL.warmup()

              warmup()

              s.join() match
                case GUIServer.Ok      ⇒ Console.ExitCodes.ok

              //newServer.stop()
          else
            browse(GUIServer.urlFile.content)
            Console.ExitCodes.ok
      case TestCompile(files) ⇒
        import org.openmole.tool.hash._

        def success(f: File) = f.getParentFileSafe / (f.hash().toString + ".success")
        def toFile(f: File) =
          if (f.isDirectory) f.listRecursive(_.isFile).toList.map(c ⇒ f -> c)
          else Seq((f.getParentFile, f))

        def isTestable(f: File) = f.getName.endsWith(".omt") || f.getName.endsWith(".oms")

        val results = Test.withTmpServices { implicit services ⇒

          import services._
          files.flatMap(toFile).filter { (_, file) ⇒ isTestable(file) }.map: (root, file) ⇒

            def processResult(c: CompileResult) =
              c match
                case s: ScriptFileDoesNotExists ⇒ util.Failure(new IOException("File doesn't exists"))
                case s: CompilationError        ⇒ util.Failure(s.error)
                case s: Compiled                ⇒ util.Success("Compilation succeeded")

            def displayName(file: File) = s"${root.relativize(file).getPath}"

            println(s"Testing: ${displayName(file)}")

            val res =
              if !success(file).exists
              then file → processResult(Project.compile(file.getParentFileSafe, file, returnUnit = true))
              else file -> util.Success("Compilation succeeded (from previous test)")

            if res._2.isSuccess then success(file) < "success"

            print("\u001b[1A\u001b[2K")
            println(s"${displayName(res._1)}: ${res._2}")

            res
          
        }

        val errors =
          results.filter:
            case (_, util.Success(_)) ⇒ false
            case _                    ⇒ true

        if (errors.isEmpty) Console.ExitCodes.ok
        else Console.ExitCodes.compilationError

  }


  def registerSignalCatcher(f: sun.misc.Signal => Unit): Unit =
    import sun.misc.*
    val handler: SignalHandler = s => f(s)
    sun.misc.Signal.handle(new Signal("TERM"), handler)
    sun.misc.Signal.handle(new Signal("INT"), handler)

}
