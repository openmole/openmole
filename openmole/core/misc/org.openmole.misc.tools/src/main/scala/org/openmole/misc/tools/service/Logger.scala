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

package org.openmole.misc.tools.service

import java.util.logging.{ Logger ⇒ JLogger, Level }

trait Logger { l ⇒

  object Log {
    @transient lazy val logger = JLogger.getLogger(l.getClass.getName)

    def SEVERE = Level.SEVERE
    def WARNING = Level.WARNING
    def INFO = Level.INFO
    def FINE = Level.FINE
    def FINER = Level.FINER
    def FINEST = Level.FINEST
  }

  @deprecated("Use log namespace instead") def logger = Log.logger
  @deprecated("Use log namespace instead") def SEVERE = Log.SEVERE
  @deprecated("Use log namespace instead") def WARNING = Log.WARNING
  @deprecated("Use log namespace instead") def INFO = Log.INFO
  @deprecated("Use log namespace instead") def FINE = Log.FINE
  @deprecated("Use log namespace instead") def FINER = Log.FINER
  @deprecated("Use log namespace instead") def FINEST = Log.FINEST
}
