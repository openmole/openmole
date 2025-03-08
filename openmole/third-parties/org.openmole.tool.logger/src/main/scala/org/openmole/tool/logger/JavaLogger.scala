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

package org.openmole.tool.logger

import java.util.logging.{ Logger => JLogger, Level }

trait Levels {
  def SEVERE = Level.SEVERE
  def WARNING = Level.WARNING
  def INFO = Level.INFO
  def FINE = Level.FINE
  def FINER = Level.FINER
  def FINEST = Level.FINEST

  def severe = SEVERE
  def warning = Level.WARNING
  def info = Level.INFO
  def fine = Level.FINE
  def finer = Level.FINER
  def finest = Level.FINEST
}

trait JavaLogger { l =>

  object Log extends Levels {
    @transient lazy val logger = JLogger.getLogger(l.getClass.getName)

    def log(level: Level, message: => String) =
      if (logger.isLoggable(level)) logger.log(level, message)
  }

}
