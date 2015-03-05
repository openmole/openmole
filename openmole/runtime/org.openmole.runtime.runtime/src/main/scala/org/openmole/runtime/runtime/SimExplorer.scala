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

package org.openmole.runtime.runtime

import org.eclipse.equinox.app._
import org.openmole.core.logging.LoggerService
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.tools.io.FileUtil
import org.openmole.core.tools.service.Logger
import org.openmole.core.workspace.Workspace
import scopt._
import FileUtil._
import java.io.File
import org.openmole.core.serializer.SerialiserService
import org.openmole.core.batch.storage._
import org.openmole.core.workflow.execution.local.LocalEnvironment
import scala.util.{ Success, Failure }

object SimExplorer extends Logger

import SimExplorer.Log._

class SimExplorer extends IApplication {

  override def start(context: IApplicationContext) = {
    try {
      val args = context.getArguments.get(IApplicationContext.APPLICATION_ARGS).asInstanceOf[Array[String]]

      case class Config(
        storage: Option[String] = None,
        inputMessage: Option[String] = None,
        outputMessage: Option[String] = None,
        path: Option[String] = None,
        pluginPath: Option[String] = None,
        nbThread: Option[Int] = None)

      val parser = new OptionParser[Config]("OpenMOLE") {
        head("OpenMOLE runtime", "0.x")
        opt[String]('s', "storage") text ("Storage") action {
          (v, c) ⇒ c.copy(storage = Some(v))
        }
        opt[String]('i', "input") text ("Path of the input message") action {
          (v, c) ⇒ c.copy(inputMessage = Some(v))
        }
        opt[String]('o', "output") text ("Path of the output message") action {
          (v, c) ⇒ c.copy(outputMessage = Some(v))
        }
        opt[String]('c', "path") text ("Path for the communication") action {
          (v, c) ⇒ c.copy(path = Some(v))
        }
        opt[String]('p', "plugin") text ("Path for plugin category to preload") action {
          (v, c) ⇒ c.copy(pluginPath = Some(v))
        }
        opt[Int]('t', "nbThread") text ("Number of thread for the execution") action {
          (v, c) ⇒ c.copy(nbThread = Some(v))
        }
      }

      val debug = args.contains("-d")
      val filteredArgs = args.filterNot((_: String) == "-d")
      if (debug) LoggerService.level("ALL")

      parser.parse(filteredArgs, Config()) foreach { config ⇒

        PluginManager.tryLoad(new File(config.pluginPath.get).listFiles)
        PluginManager.startAll

        val storageFile = Workspace.newFile("storage", ".xml")
        val storage =
          try {
            new File(config.storage.get).copyUncompressFile(storageFile)
            SerialiserService.deserialiseAndExtractFiles[RemoteStorage](storageFile)
          }
          finally storageFile.delete

        LocalEnvironment.default = LocalEnvironment(config.nbThread.getOrElse(1))

        new Runtime().apply(
          storage,
          config.path.get,
          config.inputMessage.get,
          config.outputMessage.get,
          debug)

      }
    }
    catch {
      case t: Throwable ⇒
        logger.log(SEVERE, "Error during runtime execution", t)
        throw t
    }

    IApplication.EXIT_OK

  }
  override def stop = {}

}
