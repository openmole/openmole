/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.misc

import collection.JavaConversions._
import java.io._
import org.osgi.framework._
import java.net.URLDecoder

package object osgi {

  val OpenMOLEScope = "OpenMOLE-Scope"
  val OpenMOLELocationProperty = "openmole.location"

  def openMOLELocation =
    Option(System.getProperty(OpenMOLELocationProperty, null)).map(new File(_))

  implicit class BundleDecorator(b: Bundle) {

    def isSystem = b.getLocation.toLowerCase.contains("system bundle") || b.getLocation.startsWith("netigso:")

    def isProvided = b.getHeaders.toMap.exists { case (k, v) ⇒ k.toString.toLowerCase.contains("openmole-scope") && v.toString.toLowerCase.contains("provided") }

    def file = {
      val (ref, url) = if (b.getLocation.startsWith("reference:"))
        true -> b.getLocation.substring("reference:".length)
      else if (b.getLocation.startsWith("initial@reference:")) true -> b.getLocation.substring("initial@reference:".length)
      else false -> b.getLocation

      val location = {
        val protocol = url.indexOf(':')
        val noProtocol = if (protocol == -1) url else url.substring(protocol + 1)
        if (noProtocol.endsWith("/")) noProtocol.substring(0, noProtocol.length - 1)
        else noProtocol
      }

      val decodedLocation = URLDecoder.decode(location, "UTF-8")

      if (ref)
        openMOLELocation match {
          case Some(oMLoc) ⇒ new File(oMLoc, decodedLocation)
          case None ⇒ new File(decodedLocation)
        }
      else new File(decodedLocation)
    }

  }
}
