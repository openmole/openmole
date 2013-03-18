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
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import concurrent.stm._

object KeyPrototypeGenerator {

  val _cacheMap: Ref[Option[Map[PrototypeKey, IPrototypeDataProxyUI]]] = Ref(None)

  def invalidateCache = _cacheMap.single() = None

  def cacheMap: Map[PrototypeKey, IPrototypeDataProxyUI] = atomic { implicit actx ⇒
    _cacheMap() match {
      case Some(m) ⇒ m
      case None ⇒
        val m = Map(KeyPrototypeGenerator(EmptyDataUIs.emptyPrototypeProxy) -> EmptyDataUIs.emptyPrototypeProxy) ++
          Proxys.prototypes.map { p ⇒ KeyPrototypeGenerator(p) -> p }
        _cacheMap() = Some(m)
        m
    }
  }

  def inCacheMap(k: PrototypeKey) = atomic { implicit actx ⇒ cacheMap.contains(k) }

  def apply(proxy: IPrototypeDataProxyUI): PrototypeKey =
    new PrototypeKey(proxy.dataUI.name, KeyGenerator.stripArrays(proxy.dataUI.protoType)._1.runtimeClass, proxy.dataUI.dim)

  def apply(proto: Prototype[_]): PrototypeKey = {
    val (manifest, dim) = KeyGenerator.stripArrays(proto.`type`)
    new PrototypeKey(proto.name, manifest.runtimeClass, dim)
  }

  def apply(name: String, protoClass: Class[_], dim: Int) = new PrototypeKey(name, protoClass, dim)

  def prototype(key: PrototypeKey): IPrototypeDataProxyUI = {
    buildUnknownPrototype(key)
    cacheMap(key)
  }

  def prototype(proto: Prototype[_]): IPrototypeDataProxyUI = {
    buildUnknownPrototype(proto)
    cacheMap(KeyPrototypeGenerator(proto))
  }

  def buildUnknownPrototype(k: PrototypeKey): IPrototypeDataProxyUI =
    buildUnknownPrototype(k.name, k.dim, KeyGenerator.stripArrays(ClassUtils.manifest(k.protoClass))._1)

  def buildUnknownPrototype(p: Prototype[_]): IPrototypeDataProxyUI = {
    val (_, dim) = KeyGenerator(p)
    buildUnknownPrototype(p.name, dim, KeyGenerator.stripArrays(p.`type`)._1)
  }

  def buildUnknownPrototype(name: String, dim: Int, m: Manifest[_]): IPrototypeDataProxyUI = {
    val proxy = new PrototypeDataProxyUI(GenericPrototypeDataUI(name, dim)(m), generated = true)
    if (!isPrototype(proxy)) Proxys += proxy

    proxy
  }

  def isPrototype(k: PrototypeKey): Boolean = inCacheMap(k)

  def isPrototype(p: Prototype[_]): Boolean = isPrototype(KeyPrototypeGenerator(p))

  def isPrototype(p: IPrototypeDataProxyUI): Boolean = isPrototype(KeyPrototypeGenerator(p))
}

case class PrototypeKey(val name: String, val protoClass: Class[_], val dim: Int)
