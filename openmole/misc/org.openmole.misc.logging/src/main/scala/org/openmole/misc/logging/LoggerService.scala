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

import java.io.File
import java.util.logging.Level
import java.util.logging.LogManager
import org.slf4j.bridge.SLF4JBridgeHandler
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.io.FileUtil._
import org.apache.commons.logging.Log
import collection.JavaConversions._

object LoggerService {
  private val LogLevel = new ConfigurationLocation("LoggerService", "LogLevel")

  Workspace += (LogLevel, "INFO")

  def level(levelLabel: String) = {
    val level = Level.parse(levelLabel)
    SLF4JBridgeHandler.uninstall
    SLF4JBridgeHandler.removeHandlersForRootLogger
    //val handlers = rootLogger.getHandlers
    //for (h ← handlers) rootLogger.removeHandler(h)
    SLF4JBridgeHandler.install
    
    val rootLogger = LogManager.getLogManager.getLogger("")
    rootLogger.setLevel(level)
  }

  def init = level(Workspace.preference(LogLevel))
}
