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

package object buildinfo:

  def name = "Xtreme Xploration"

  case class Version(value: String, name: String, time: Long):
    override def toString = value
    def major = value.takeWhile(_.isDigit)
    def minor = value.drop(major.size + 1).takeWhile(_.isDigit)
    def isDevelopment = value.toLowerCase.endsWith("snapshot")
    def generationDate =
      val d = Calendar.getInstance()
      d.setTimeInMillis(BuildInfo.buildTime)
      val format = DateFormat.getDateInstance(DateFormat.LONG, new Locale("EN", "en"))
      format.format(d.getTime)

    def generationTime: String =
      val d = Calendar.getInstance()
      d.setTimeInMillis(BuildInfo.buildTime)
      val format = DateFormat.getTimeInstance(DateFormat.LONG, new Locale("EN", "en"))
      format.format(d.getTime)

  def version = Version(BuildInfo.version, name, BuildInfo.buildTime)
  def development = version.isDevelopment

  def consoleSplash = BuildInfo.splash

  def siteURL =
    development match
      case true  ⇒ "http://next.openmole.org"
      case false ⇒ s"http://openmole.org/all/${version.value}"

  def marketName = "market.json"
  def marketAddress = marketURL(marketName)
  def marketURL(entry: String) = url(s"market/$entry")

  def moduleListName = "modules.json"
  def moduleAddress = url(s"modules/$moduleListName")

  def url(entry: String): String = siteURL + "/" + entry
  def scalaVersion = BuildInfo.scalaVersion


