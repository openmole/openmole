/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.misc.logging

import org.apache.log4j.{ Logger ⇒ L4JLogger, Level ⇒ L4JLevel, Appender ⇒ L4JAppender }
import org.apache.log4j.BasicConfigurator
import java.util.logging._

import org.openmole.misc.workspace._
import org.openmole.misc.tools.io.FileUtil._
import collection.JavaConversions._

object LoggerService {

  //val blackList = TSet[String]("net.schmizz.sshj")

  private val LogLevel = new ConfigurationLocation("LoggerService", "LogLevel")
  Workspace += (LogLevel, "INFO")

  def level(levelLabel: String) = {
    val level = Level.parse(levelLabel)

    LogManager.getLogManager.reset

    val rootLogger = Logger.getLogger("")
    rootLogger.setLevel(level)

    val ch = new ConsoleHandler
    ch.setLevel(level)
    /* ch.setFilter(
      new Filter {
        def isLoggable(record: LogRecord) = {
          println("Loggable " + record.getSourceClassName)
          !blackList.single.exists(record.getSourceClassName.startsWith)
        }
      })  */

    rootLogger.addHandler(ch)

  }

  def init = {
    BasicConfigurator.configure
    L4JLogger.getRootLogger.setLevel(L4JLevel.ERROR)
    val root = org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[ch.qos.logback.classic.Logger]
    root.setLevel(ch.qos.logback.classic.Level.ERROR)
    level(Workspace.preference(LogLevel))
  }

}
