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

package org.openmole.core

import java.io._
import java.net.URLDecoder

import org.openmole.core.location._
import org.osgi.framework._
import org.osgi.framework.wiring.BundleWiring

import scala.jdk.CollectionConverters._

package object pluginmanager {

  val OpenMOLEScope = "OpenMOLE-Scope"

  implicit class BundleDecorator(b: Bundle) {

    def isSystem = b.getLocation.toLowerCase.contains("system bundle") || b.getLocation.startsWith("netigso:")
    def headerExists(f: (String, String) => Boolean) = b.getHeaders.asScala.toSeq.exists { case (k, v) => f(k, v) }

    def openMOLEScope: Seq[String] =
      b.getHeaders.asScala.toSeq.find { case (k, v) => k.toLowerCase == "openmole-scope" }.toSeq.flatMap(_._2.split(","))
    def isProvided = openMOLEScope.exists(_.toLowerCase == "provided")

    def isFullDynamic = headerExists { (k, v) => k.contains("DynamicImport-Package") && v.split(",").toSet.contains("*") }

    def file = {
      val (ref, url) = if (b.getLocation.startsWith("reference:"))
        true → b.getLocation.substring("reference:".length)
      else if (b.getLocation.startsWith("initial@reference:")) true → b.getLocation.substring("initial@reference:".length)
      else false → b.getLocation

      val location = {
        val protocol = url.indexOf(':')
        val noProtocol = if (protocol == -1) url else url.substring(protocol + 1)
        if (noProtocol.endsWith("/")) noProtocol.substring(0, noProtocol.length - 1)
        else noProtocol
      }

      val decodedLocation = location //URLDecoder.decode(location, "UTF-8")

      if (ref)
        openMOLELocationOption match {
          case Some(oMLoc) => new File(oMLoc, decodedLocation)
          case None        => new File(decodedLocation)
        }
      else new File(decodedLocation)
    }

    def classLoader = b.adapt(classOf[BundleWiring]).getClassLoader
  }
}
