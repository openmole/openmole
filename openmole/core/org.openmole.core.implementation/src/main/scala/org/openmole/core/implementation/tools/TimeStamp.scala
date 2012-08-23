/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.implementation.tools

import org.openmole.core.model.tools.ITimeStamp
import org.openmole.core.model.job.State._
import org.openmole.misc.tools.service.LocalHostName

class TimeStamp[T](val state: T, val hostName: String, val time: Long) extends ITimeStamp[T] {
  def this(state: T, time: Long) = this(state, LocalHostName.localHostName, time)
  def this(state: T) = this(state, LocalHostName.localHostName, System.currentTimeMillis)
}
