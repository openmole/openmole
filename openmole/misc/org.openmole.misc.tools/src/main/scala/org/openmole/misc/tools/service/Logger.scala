/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.tools.service

import java.util.logging.{ Logger ⇒ JLogger, Level }

trait Logger {
  @transient lazy val logger = JLogger.getLogger(getClass.getName)

  def SEVERE = Level.SEVERE
  def WARNING = Level.WARNING
  def INFO = Level.INFO
  def FINE = Level.FINE
  def FINER = Level.FINER
  def FINEST = Level.FINEST
}
