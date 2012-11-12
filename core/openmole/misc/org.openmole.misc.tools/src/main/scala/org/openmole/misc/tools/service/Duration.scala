/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.misc.tools.service

import org.joda.time.format.ISOPeriodFormat

object Duration {
  implicit class DurationStringDecorator(s: String) {
    def toSeconds = ISOPeriodFormat.standard.parsePeriod(s).toStandardSeconds.getSeconds
    def toMilliSeconds = toSeconds * 1000L
    def toMinutes = ISOPeriodFormat.standard.parsePeriod(s).toStandardMinutes.getMinutes
    def toDays = ISOPeriodFormat.standard.parsePeriod(s).toStandardDays.getDays
  }
}
