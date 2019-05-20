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

  def log(msg: ⇒ String, level: Level = Level.INFO)(implicit loggerService: LoggerService, name: FullName, line: Line, outputRedirection: OutputRedirection) =
    if (level.intValue() > loggerService.level.intValue()) {
      OutputRedirection.println(s"""${name.value}:${line.value} - $msg""")
    }

}

case class LoggerService(level: Level = Level.WARNING)

