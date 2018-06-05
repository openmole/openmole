package org.openmole.site

/*
 * Copyright (C) 2015 Romain Reuillon
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

object Config {
  val baseURL = "http://www.openmole.org"

  lazy val closedIssues = Map(
    "7" → "https://github.com/openmole/openmole/milestone/3?closed=1",
    "6.2" → "",
    "6.1" → "https://github.com/openmole/openmole/milestone/6?closed=1",
    "6.0" → "https://github.com/openmole/openmole/milestone/2?closed=1"
  )
}
