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

object BuddyClassLoader:
  import org.osgi.framework.Bundle
  import org.osgi.framework.wiring.{BundleRevision, BundleWiring}
  import scala.jdk.CollectionConverters.*

  def isClassExported(bundle: Bundle, className: String): Boolean =
    val idx = className.lastIndexOf('.')
    if idx < 0
    then false
    else
      val pkg = className.substring(0, idx)

      bundle
        .adapt(classOf[BundleWiring])
        .getCapabilities(BundleRevision.PACKAGE_NAMESPACE)
        .asScala
        .exists: cap =>
          cap.getAttributes.get(BundleRevision.PACKAGE_NAMESPACE) == pkg


class BuddyClassLoader(owner: Bundle, includePrivate: Boolean = false) extends ClassLoader():

  def orderedBundles =
    lazy val dependencies = PluginManager.allDependencies(owner)
    def otherBundles =
      val deps = dependencies.map(_.getBundleId).toSet
      PluginManager.bundles.filterNot(b => deps.contains(b.getBundleId))

    LazyList(owner) lazyAppendedAll dependencies lazyAppendedAll otherBundles

  override def loadClass(name: String, resolve: Boolean): Class[?] =
    def lookForClass(includePrivate: Boolean) =
      Option(findLoadedClass(name)).orElse:
        orderedBundles.view.flatMap: b =>
          if includePrivate || BuddyClassLoader.isClassExported(b, name)
          then tryOption(b.loadClass(name))
          else None
        .headOption

    TypeTool.primitiveType(name).orElse:
      if includePrivate
      then lookForClass(false) orElse lookForClass(true)
      else lookForClass(false)
    .getOrElse(throw ClassNotFoundException(name))

  override def findResource(name: String): URL =
    orderedBundles.view.flatMap: b =>
      Option(b.getResource(name))
    .headOption
    .orNull

  private def tryOption[T](block: => T): Option[T] =
    try Some(block)
    catch case _: ClassNotFoundException => None

