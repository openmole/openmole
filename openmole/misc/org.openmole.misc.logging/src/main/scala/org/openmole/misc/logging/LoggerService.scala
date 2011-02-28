/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.logging

import java.util.logging.Level
import java.util.logging.LogManager
import org.slf4j.bridge.SLF4JBridgeHandler

object LoggerService {
  def setLevel(level: Level) = {
    SLF4JBridgeHandler.uninstall
    
    val rootLogger = LogManager.getLogManager.getLogger("")
    val handlers = rootLogger.getHandlers
    for (h <- handlers) rootLogger.removeHandler(h)

    SLF4JBridgeHandler.install

    for (i <- 0 until handlers.length) {
      rootLogger.setLevel(level)
    }
  }
}
