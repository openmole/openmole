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
import java.util.{ Calendar, Locale }
import fr.iscpif.gridscale.http.HTTPStorage

package object buildinfo {

  def name = "M... M..."

  case class Version(value: String, name: String, time: Long) {
    override def toString = value
    def major = value.takeWhile(_.isDigit)
    def minor = value.drop(major.size + 1).takeWhile(_.isDigit)
    def isDevelopment = value.toLowerCase.endsWith("snapshot")
    def generationDate = {
      val d = Calendar.getInstance()
      d.setTimeInMillis(BuildInfo.buildTime)
      val format = DateFormat.getDateInstance(DateFormat.LONG, new Locale("EN", "en"))
      format.format(d.getTime)
    }
  }

  def version = Version(BuildInfo.version, name, BuildInfo.buildTime)

  def development = version.isDevelopment

  def siteURL =
    development match {
      case true  ⇒ "http://next.openmole.org"
      case false ⇒ s"http://www.openmole.org/all/$version"
    }

  import org.json4s._
  import org.json4s.jackson.Serialization
  implicit val formats = Serialization.formats(NoTypeHints)

  def marketName = "market.json"
  def marketAddress = url(marketName)
  def marketIndex = HTTPStorage.download(buildinfo.marketAddress)(Serialization.read[buildinfo.MarketIndex](_))

  def moduleListName = "modules.json"
  def moduleAddress = url(moduleListName)

  def url(entry: String): String = siteURL + "/" + entry

  //def info = OpenMOLEBuildInfo(version, name, generationDate)

}
