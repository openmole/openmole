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

  val plugins = new ConcurrentHashMap[AnyRef, PluginInfo]().asScala

  def addPlugin(c: AnyRef, info: PluginInfo) = plugins += c → info
  def removePlugin(c: AnyRef) = plugins -= c
  def pluginsInfo = plugins.values

  def register(id: AnyRef, namespaces: Vector[NameSpace] = Vector(), keywordTraits: Vector[KeyWordTrait] = Vector.empty, keyWords: Vector[KeyWord] = Vector()): Unit = {
    val info = PluginInfo(namespaces.map(_.value), keywordTraits.map(_.value), keyWords)
    PluginInfo.addPlugin(id, info)
  }

  def unregister(id: AnyRef): Unit = PluginInfo.removePlugin(id)

  object NameSpace {
    implicit def apply(p: Package): NameSpace = NameSpace(p.getName)
  }
  case class NameSpace(value: String)

  object KeyWordTrait {
    implicit def apply(c: Class[_]): KeyWordTrait = KeyWordTrait(c.getCanonicalName)
  }

  case class KeyWordTrait(value: String)

  def keyWords = pluginsInfo.flatMap(_.keyWords)
  def nameSpaces = pluginsInfo.flatMap(_.namespaces)
  def keyWordTraits = pluginsInfo.flatMap(_.keyWordTraits)
}

case class PluginInfo(
  namespaces:    Vector[String],
  keyWordTraits: Vector[String],
  keyWords:      Vector[KeyWord])

sealed trait KeyWord {
  def name: String
}

object KeyWord {
  implicit def classToString(c: Class[_]) = c.getSimpleName
  case class Task(name: String) extends KeyWord
  case class Hook(name: String) extends KeyWord
  case class Source(name: String) extends KeyWord
  case class Environment(name: String) extends KeyWord
  case class Setter(name: String) extends KeyWord
  case class Adder(name: String) extends KeyWord
  case class Pattern(name: String) extends KeyWord
  case class Transition(name: String) extends KeyWord
  case class Sampling(name: String) extends KeyWord
  case class Word(name: String) extends KeyWord
}

trait PluginInfoActivator extends BundleActivator {
  def keyWordTraits: List[Class[_]] = Nil
  override def start(bundleContext: BundleContext): Unit = PluginInfo.register(this, Vector(this.getClass.getPackage), keyWordTraits.toVector.map(PluginInfo.KeyWordTrait(_)))
  override def stop(bundleContext: BundleContext): Unit = PluginInfo.unregister(this)
}

