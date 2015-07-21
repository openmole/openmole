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

  def name = "L... L..."

  def version: String = buildinfo.BuildInfo.version
  def versionNumber: String = version.takeWhile(_ != "-")

  def generationDate = {
    val d = Calendar.getInstance()
    d.setTimeInMillis(buildinfo.BuildInfo.buildTime)
    val format = DateFormat.getDateInstance(DateFormat.LONG, new Locale("EN", "en"))
    format.format(d.getTime)
  }

  def development = version.toLowerCase.endsWith("snapshot")

  def siteURL =
    development match {
      case true  ⇒ "next.openmole.org"
      case false ⇒ s"openmole.org/all/$versionNumber"
    }

  def marketName = "market.xml"
  def marketAddress = s"http://$siteURL/$marketName"

}
