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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.pluginmanager

import java.util.concurrent._

import org.osgi.framework.{ BundleActivator, BundleContext }

import scala.collection.JavaConverters._

object PluginInfo {
  val plugins = new ConcurrentHashMap[Class[_], PluginInfo]().asScala
  def addPlugin(c: Class[_], info: PluginInfo) = plugins += c → info
  def removePlugin(c: Class[_]) = plugins -= c
  def pluginsInfo = plugins.values

  def add(clazz: Class[_], keywordTraits: List[Class[_]] = Nil): Unit = {
    val info = PluginInfo(List(clazz.getPackage.getName), keywordTraits.map(_.getCanonicalName))
    PluginInfo.addPlugin(clazz, info)
  }

  def remove(clazz: Class[_]): Unit = PluginInfo.removePlugin(clazz)
}

case class PluginInfo(namespaces: List[String], keywordTraits: List[String])

trait PluginInfoActivator extends BundleActivator {
  def keyWordTraits: List[Class[_]] = Nil
  override def start(bundleContext: BundleContext): Unit = PluginInfo.add(this.getClass, keyWordTraits)
  override def stop(bundleContext: BundleContext): Unit = PluginInfo.remove(this.getClass)
}
