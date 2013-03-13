/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.registry

import org.openmole.misc.tools.obj.ClassUtils
import org.openmole.core.model.data._
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxys }
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import scala.collection.mutable.WeakHashMap
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI

object KeyPrototypeGenerator {

  private val cacheMap = new WeakHashMap[PrototypeKey, IPrototypeDataProxyUI]
  cacheMap += (KeyPrototypeGenerator(EmptyDataUIs.emptyPrototypeProxy) -> EmptyDataUIs.emptyPrototypeProxy)

  def apply(proxy: IPrototypeDataProxyUI): PrototypeKey =
    new PrototypeKey(proxy.dataUI.name, KeyGenerator.stripArrays(proxy.dataUI.protoType)._1.runtimeClass, proxy.dataUI.dim)

  def apply(proto: Prototype[_]): PrototypeKey = {
    val (manifest, dim) = KeyGenerator.stripArrays(proto.`type`)
    new PrototypeKey(proto.name, manifest.runtimeClass, dim)
  }

  def prototype(key: PrototypeKey): IPrototypeDataProxyUI = {
    cacheMap.getOrElseUpdate(key, buildUnknownPrototype(key))
  }

  def prototype(proto: Prototype[_]): IPrototypeDataProxyUI =
    cacheMap.getOrElseUpdate(KeyPrototypeGenerator(proto), buildUnknownPrototype(proto))

  def buildUnknownPrototype(k: PrototypeKey): IPrototypeDataProxyUI =
    buildUnknownPrototype(k.name, k.dim, KeyGenerator.stripArrays(ClassUtils.manifest(k.protoClass))._1)

  def buildUnknownPrototype(p: Prototype[_]): IPrototypeDataProxyUI = {
    val (_, dim) = KeyGenerator(p)
    buildUnknownPrototype(p.name, dim, KeyGenerator.stripArrays(p.`type`)._1)
  }

  def buildUnknownPrototype(name: String, dim: Int, m: Manifest[_]): IPrototypeDataProxyUI = {
    val proxy = new PrototypeDataProxyUI(GenericPrototypeDataUI(name, dim)(m), generated = true)
    Proxys += proxy
    proxy
  }

  def keyPrototypeMapping: Map[PrototypeKey, IPrototypeDataProxyUI] = cacheMap.toMap

  def isPrototype(k: PrototypeKey): Boolean = cacheMap.keys.toList.contains(k)

  def isPrototype(p: Prototype[_]): Boolean = isPrototype(KeyPrototypeGenerator(p))

  def isPrototype(p: IPrototypeDataProxyUI): Boolean = isPrototype(KeyPrototypeGenerator(p))
}

case class PrototypeKey(val name: String, val protoClass: Class[_], val dim: Int)
