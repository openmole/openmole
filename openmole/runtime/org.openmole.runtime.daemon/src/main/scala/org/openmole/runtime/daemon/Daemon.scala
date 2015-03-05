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

package org.openmole.runtime.daemon

import org.codehaus.groovy.vmplugin.v5.PluginDefaultGroovyMethods
import org.eclipse.equinox.app._
import org.openmole.core.tools.service.Logger
import scopt._

object Daemon extends Logger

import Daemon.Log._

class Daemon extends IApplication {
  override def start(context: IApplicationContext) = {
    try {
      val args = context.getArguments.get(IApplicationContext.APPLICATION_ARGS).asInstanceOf[Array[String]]

      case class Config(
        host: Option[String] = None,
        password: Option[String] = None,
        workers: Int = 1,
        cacheSize: Int = 2000)

      val parser = new OptionParser[Config]("OpenMOLE") {
        head("OpenMOLE deamon", "0.x")
        opt[String]('h', "host") text ("user@hostname:port") action {
          (v, c) ⇒ c.copy(host = Some(v))
        }
        opt[String]('p', "password") text ("password") action {
          (v, c) ⇒ c.copy(password = Some(v))
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
        new JobLauncher(config.cacheSize * 1024 * 1024, debug).launch(
          config.host.getOrElse(throw new RuntimeException("Host undefined")),
          config.password.getOrElse(throw new RuntimeException("Password undefined")),
          config.workers)
      }
    }
    catch {
      case t: Throwable ⇒ logger.log(SEVERE, "Error during daemon execution", t)
    }
    IApplication.EXIT_OK
  }

  override def stop = {}

}
