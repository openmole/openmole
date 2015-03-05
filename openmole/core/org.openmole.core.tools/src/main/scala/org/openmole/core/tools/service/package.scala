/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.tools

import org.joda.time.format.ISOPeriodFormat
import concurrent.duration._

package object service {

  def localHostName = LocalHostName.localHostName
  def newRNG(seed: Long) = Random.newRNG(seed)

  implicit def stringToDuration(s: String): FiniteDuration = ISOPeriodFormat.standard.parsePeriod(s).toStandardSeconds.getSeconds seconds

  implicit class StringDurationDecorator(s: String) {
    def toDuration = stringToDuration(s)
  }
}