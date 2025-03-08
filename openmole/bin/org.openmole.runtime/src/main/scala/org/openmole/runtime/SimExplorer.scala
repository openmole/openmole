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

package org.openmole.runtime

import org.openmole.core.logconfig.*
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.tool.file.*
import org.openmole.tool.logger.{JavaLogger, LoggerService}
import scopt.*

import java.io.File
import java.util.logging.Level
import org.openmole.core.serializer.SerializerService
import org.openmole.core.communication.storage.RemoteStorage
import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.{FileService, FileServiceCache}
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.timeservice.TimeService
import org.openmole.core.workspace.{TmpDirectory, Workspace}

object SimExplorer extends JavaLogger {

  import Log._

  def run(args: Array[String]): Int = {
    try {
      logger.finest("Running OpenMOLE runtime")

      case class Config(
        storage:       Option[String] = None,
        inputMessage:  Option[String] = None,
        outputMessage: Option[String] = None,
        pluginPath:    Option[String] = None,
        thread:        Option[Int]    = None,
        workspace:     Option[String] = None,
        transferRetry: Option[Int]    = None,
        test:          Boolean        = false,
        debug:         Boolean        = false
      )

      val parser = new OptionParser[Config]("OpenMOLE") {
        head("OpenMOLE runtime", "0.x")
        opt[String]('s', "storage") text ("Storage") action {
          (v, c) => c.copy(storage = Some(v))
        }
        opt[String]('i', "input") text ("Path of the input message") action {
          (v, c) => c.copy(inputMessage = Some(v))
        }
        opt[String]('o', "output") text ("Path of the output message") action {
          (v, c) => c.copy(outputMessage = Some(v))
        }
        opt[String]('p', "plugin") text ("Path for plugin category to preload") action {
          (v, c) => c.copy(pluginPath = Some(v))
        }
        opt[Int]('t', "thread") text ("Number of threads for the execution") action {
          (v, c) => c.copy(thread = Some(v))
        }
        opt[String]('w', "workspace") text ("Workspace location") action {
          (v, c) => c.copy(workspace = Some(v))
        }
        opt[Int]("transfer-retry") text ("Retry fail transfer on failure") action {
          (v, c) => c.copy(transferRetry = Some(v))
        }
        opt[Unit]('d', "debug") text ("Switch on the debug mode") action {
          (_, c) => c.copy(debug = true)
        }
        opt[Unit]("test") text ("Switch on test mode") action {
          (_, c) => c.copy(test = true)
        }
      }

      parser.parse(args, Config()) foreach { config =>
        config.test match {
          case false =>

            if (config.debug) LoggerConfig.level(Level.FINEST)

            val threads = config.thread.getOrElse(1)
            logger.fine(s"running with: $threads threads")

            implicit val workspace: Workspace = Workspace(new File(config.workspace.get).getCanonicalFile)
            implicit val newFile: TmpDirectory = TmpDirectory(workspace)
            implicit val serializerService: SerializerService = SerializerService()
            implicit val preference: Preference = Preference.memory()
            implicit val threadProvider: ThreadProvider = ThreadProvider(threads + 5)
            implicit val fileService: FileService = FileService()
            implicit val fileServiceCache: FileServiceCache = FileServiceCache()
            implicit val eventDispatcher: EventDispatcher = EventDispatcher()
            implicit val loggerService: LoggerService = if (config.debug) LoggerService(level = Some(finest)) else LoggerService()
            implicit val networkService: NetworkService = NetworkService(None)
            implicit val timeService: TimeService = TimeService()

            try
              PluginManager.startAll.foreach { case (b, e) => logger.log(WARNING, s"Error starting bundle $b", e) }
              PluginManager.tryLoad(new File(config.pluginPath.get).listFilesSafe).foreach { case (f, e) => logger.log(WARNING, s"Error loading bundle $f", e) }

              logger.fine("plugins: " + config.pluginPath.get + " " + new File(config.pluginPath.get).listFilesSafe.mkString(","))

              val storage = serializerService.deserializeAndExtractFiles[RemoteStorage](new File(config.storage.get), deleteFilesOnGC = true, gz = true)

              new Runtime().apply(
                storage,
                config.inputMessage.get,
                config.outputMessage.get,
                threads,
                config.debug,
                config.transferRetry
              )
            finally threadProvider.stop()
          case true => logger.info("The runtime is working")
        }

      }
    }
    catch {
      case t: Throwable =>
        logger.log(SEVERE, "Error during runtime execution", t)
        throw t
    }

    0
  }

}
