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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.core

import java.text.DateFormat
import java.util.{ Locale, Calendar }

package object buildinfo {

  def name = "M... M..."

  def version: String = buildinfo.BuildInfo.version

  def generationDate = {
    val d = Calendar.getInstance()
    d.setTimeInMillis(buildinfo.BuildInfo.buildTime)
    val format = DateFormat.getDateInstance(DateFormat.LONG, new Locale("EN", "en"))
    format.format(d.getTime)
  }

  def development = version.toLowerCase.endsWith("snapshot")

  def siteURL =
    development match {
      case true  ⇒ "http://next.openmole.org"
      case false ⇒ s"http://openmole.org/all/$version"
    }

  def marketName = "market.bin"
  def marketAddress = url(marketName)
  def url(entry: String): String = siteURL + "/" + entry

  def info = OpenMOLEBuildInfo(version, name, generationDate)

}
