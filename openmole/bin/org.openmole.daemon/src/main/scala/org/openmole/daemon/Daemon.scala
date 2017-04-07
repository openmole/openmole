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

package org.openmole.daemon

import org.openmole.core.fileservice.FileService
import org.openmole.core.logging._
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.tool.file._
import org.openmole.tool.logger._
import org.openmole.core.services._
import org.openmole.core.threadprovider.ThreadProvider
import scopt._

object Daemon extends Logger {

  import Log._

  def run(args: Array[String]) = {
    try {
      case class Config(
        host:      Option[String] = None,
        password:  Option[String] = None,
        workspace: Option[File]   = None,
        workers:   Int            = 1,
        cacheSize: Int            = 2000
      )

      val parser = new OptionParser[Config]("OpenMOLE") {
        head("OpenMOLE deamon", "0.x")
        opt[String]('h', "host") text ("hostname:port") action {
          (v, c) ⇒ c.copy(host = Some(v))
        }
        opt[String]('p', "password") text ("password") action {
          (v, c) ⇒ c.copy(password = Some(v))
        }
        opt[String]('w', "workspace") text ("workspace") action {
          (v, c) ⇒ c.copy(workspace = Some(File(v)))
        }
        opt[Int]('w', "workers") text ("Number of workers, default is 1") action {
          (v, c) ⇒ c.copy(workers = v)
        }
        opt[Int]('c', "cache") text ("Cache size in MB, default is 2000") action {
          (v, c) ⇒ c.copy(cacheSize = v)
        }
      }

      val debug = args.contains("-d")
      val filteredArgs = args.filterNot((_: String) == "-d")
      parser.parse(filteredArgs, Config()) foreach { config ⇒
        val workspace = Workspace(config.workspace.getOrElse(org.openmole.core.db.defaultOpenMOLEDirectory))
        implicit val preference = Services.preference(workspace)
        implicit val serializerService = SerializerService()
        implicit val newFile = NewFile(workspace)
        implicit val threadProvider = ThreadProvider(10)
        implicit val fileService = FileService()

        try {
          val lancher = new JobLauncher(config.cacheSize * 1024 * 1024, debug)
          lancher.launch(
            config.host.getOrElse(throw new RuntimeException("Host undefined")),
            config.password.getOrElse(throw new RuntimeException("Password undefined")),
            config.workers
          )
        }
        finally {
          workspace.tmpDir.recursiveDelete
          threadProvider.stop()
        }
      }
    }
    catch {
      case t: Throwable ⇒ logger.log(SEVERE, "Error during daemon execution", t)
    }

    0
  }

}
