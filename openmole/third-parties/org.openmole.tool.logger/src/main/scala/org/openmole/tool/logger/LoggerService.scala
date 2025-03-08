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

import org.openmole.tool.outputredirection._
import sourcecode._

import java.io.{ BufferedOutputStream, FileOutputStream, PrintStream }
import java.util.logging.Level
import java.util.zip.GZIPOutputStream

object LoggerService {

  def log(l: Level, msg: => String, exception: Option[Throwable] = None)(implicit name: FullName, line: Line, loggerService: LoggerService, outputRedirection: OutputRedirection) = {
    def outputError(p: PrintStream) = {
      p.println(s"""${name.value}:${line.value} - $msg""")
      exception match {
        case Some(e) =>
          p.print(s"Caused by: ${e}")
          e.printStackTrace(p)
        case None =>
      }
    }

    if (l.intValue() >= loggerService.level.getOrElse(Level.WARNING).intValue()) outputError(outputRedirection.error)

    (loggerService.file, loggerService.fileLevel) match {
      case (Some(f), Some(ll)) if l.intValue() >= ll.intValue() =>
        f.getParentFile.mkdirs()
        val ps = new PrintStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(f, true))))
        try outputError(ps)
        finally ps.close()
      case _ =>
    }
  }

  def fine(msg: => String, exception: Throwable)(implicit name: FullName, line: Line, loggerService: LoggerService, outputRedirection: OutputRedirection) =
    log(Level.FINE, msg, Some(exception))

  def fine(msg: => String)(implicit name: FullName, line: Line, loggerService: LoggerService, outputRedirection: OutputRedirection) =
    log(Level.FINE, msg, None)

  def apply(
    level:     Option[Level]        = None,
    file:      Option[java.io.File] = None,
    fileLevel: Option[Level]        = None) = new LoggerService(level, file, fileLevel)

}

case class LoggerService(
  level:     Option[Level],
  file:      Option[java.io.File],
  fileLevel: Option[Level])
