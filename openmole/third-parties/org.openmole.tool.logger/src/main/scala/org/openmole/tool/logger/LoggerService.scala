package org.openmole.tool.logger

/*
 * Copyright (C) 2019 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.logging.Level

import org.openmole.tool.outputredirection._
import sourcecode._

object LoggerService {
  def log(l: Level, msg: ⇒ String, exception: Option[Throwable] = None)(implicit name: FullName, line: Line, loggerService: LoggerService, outputRedirection: OutputRedirection) =
    if (l.intValue() >= loggerService.level.getOrElse(Level.WARNING).intValue()) {
      outputRedirection.error.println(s"""${name.value}:${line.value} - $msg""")
      exception match {
        case Some(e) ⇒
          outputRedirection.error.print(s"Caused by: ${e}")
          e.printStackTrace(outputRedirection.error)
        case None ⇒
      }
    }

  def apply(level: Level): LoggerService = LoggerService(Some(level))
}

case class LoggerService(level: Option[Level] = None)
