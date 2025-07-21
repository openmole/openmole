package org.openmole.core.pluginmanager

/*
 * Copyright (C) 2025 Romain Reuillon
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


import org.osgi.framework.{Bundle, BundleContext}
import org.osgi.framework.wiring.{BundleWiring, BundleWire}

import java.net.URL
import java.util
import scala.jdk.CollectionConverters.*

class BuddyClassLoader(owner: Bundle) extends ClassLoader():

  def dependencies = PluginManager.allDependencies(owner)
  def otherBundles =
    val deps = dependencies.map(_.getBundleId).toSet
    PluginManager.bundles.filterNot(b => deps.contains(b.getBundleId))

  def orderedBundles = Seq(owner) ++ dependencies ++ otherBundles

  override def loadClass(name: String, resolve: Boolean): Class[?] =
    val c =
      Option(findLoadedClass(name)).orElse:
        orderedBundles.view.flatMap: b =>
          tryOption(b.classLoader.loadClass(name))
        .headOption
      .getOrElse(throw ClassNotFoundException(name))

    if resolve then resolveClass(c)

    c

  override def findResource(name: String): URL =
    orderedBundles.view.flatMap: b =>
      Option(b.getResource(name))
    .headOption
    .orNull

  private def tryOption[T](block: => T): Option[T] =
    try Some(block)
    catch case _: ClassNotFoundException => None

