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

import org.eclipse.equinox.app._
import org.openmole.misc.tools.service.Logger
import scopt.immutable._

class Daemon extends IApplication with Logger {
  override def start(context: IApplicationContext) = {
    try {
      val args = context.getArguments.get(IApplicationContext.APPLICATION_ARGS).asInstanceOf[Array[String]]

      case class Config(
        host: Option[String] = None,
        password: Option[String] = None,
        workers: Int = 1,
        cacheSize: Int = 2000)

      val parser = new OptionParser[Config]("openmole", "0.x") {
        def options = Seq(
          opt("h", "host", "user@hostname:port") {
            (v: String, c: Config) ⇒ c.copy(host = Some(v))
          },
          opt("p", "password", "password") {
            (v: String, c: Config) ⇒ c.copy(password = Some(v))
          },
          opt("w", "workers", "Number of workers, default is 1") {
            (v: String, c: Config) ⇒ c.copy(workers = v.toInt)
          },
          opt("c", "cache", "Chache size, default is 2000") {
            (v: String, c: Config) ⇒ c.copy(cacheSize = v.toInt)
          })
      }

      val debug = args.contains("-d")
      val filtredArgs = args.filterNot((_: String) == "-d")
      parser.parse(filtredArgs, Config()) foreach { config ⇒
        new JobLauncher(config.cacheSize * 1024 * 1024, debug).launch(
          config.host.getOrElse(throw new RuntimeException("Host undifined")),
          config.password.getOrElse(throw new RuntimeException("Password undifined")),
          config.workers)
      }
    } catch {
      case t: Throwable ⇒ logger.log(SEVERE, "Error durring runtime execution", t)
    }
    IApplication.EXIT_OK
  }

  override def stop = {}

}
