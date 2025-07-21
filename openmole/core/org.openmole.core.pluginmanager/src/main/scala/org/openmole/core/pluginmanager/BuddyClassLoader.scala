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


import org.openmole.tool.types.TypeTool
import org.osgi.framework.{Bundle, BundleContext}

import java.net.URL

class BuddyClassLoader(owner: Bundle) extends ClassLoader():

  def orderedBundles =
    lazy val dependencies = PluginManager.allDependencies(owner)
    def otherBundles =
      val deps = dependencies.map(_.getBundleId).toSet
      PluginManager.bundles.filterNot(b => deps.contains(b.getBundleId))

    LazyList(owner) lazyAppendedAll dependencies lazyAppendedAll otherBundles

  override def loadClass(name: String, resolve: Boolean): Class[?] =
    def update =
      Option(findLoadedClass(name)).orElse:
        orderedBundles.view.flatMap: b =>
          tryOption(b.classLoader.loadClass(name))
        .headOption

    TypeTool.primitiveType(name).getOrElse:
      PluginManager.synchronized:
        PluginManager.classes.getOrElseUpdate((owner.getBundleId, name), update)
      .getOrElse(throw ClassNotFoundException(name))

  override def findResource(name: String): URL =
    orderedBundles.view.flatMap: b =>
      Option(b.getResource(name))
    .headOption
    .orNull

  private def tryOption[T](block: => T): Option[T] =
    try Some(block)
    catch case _: ClassNotFoundException => None

