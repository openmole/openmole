package org.openmole.site.content.explore

/*
 * Copyright (C) 2024 Romain Reuillon
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

import org.openmole.site.content.header.*


object Evolution:
  val hookOptions: Frag = ul(
    li{html"${code{"output"}}: the file in which to store the result,"},
    li{html"${code{"keepHistory"}}: $optional, Boolean, keep the history of the results for future analysis,"},
    li{html"${code{"frequency"}}: $optional, Long, the frequency in generations where the result should be saved in the history, it is generally set to avoid using too much disk space,"},
    li{html"${code{"keepAll"}}: $optional, Boolean, save all the individuals of the population not only the optimal ones."},
  )