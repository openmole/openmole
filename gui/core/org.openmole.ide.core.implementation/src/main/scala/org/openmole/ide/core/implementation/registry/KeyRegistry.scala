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

import org.openmole.ide.core.model.factory._
import org.openmole.misc.tools.obj.ClassUtils.ClassDecorator
import org.openmole.ide.core.implementation.dataproxy.Proxys
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import org.openmole.ide.core.model.dataproxy.ISamplingCompositionDataProxyUI
import org.openmole.misc.exception.UserBadDataError

object KeyRegistry {
  val prototypes = new HashMap[PrototypeKey, IPrototypeFactoryUI] with SynchronizedMap[PrototypeKey, IPrototypeFactoryUI]

  val tasks = new HashMap[DefaultKey, ITaskFactoryUI] with SynchronizedMap[DefaultKey, ITaskFactoryUI]

  val samplings = new HashMap[DefaultKey, ISamplingFactoryUI] with SynchronizedMap[DefaultKey, ISamplingFactoryUI]

  val environments = new HashMap[DefaultKey, IEnvironmentFactoryUI] with SynchronizedMap[DefaultKey, IEnvironmentFactoryUI]

  val domains = new HashMap[DefaultKey, IDomainFactoryUI] with SynchronizedMap[DefaultKey, IDomainFactoryUI]

  val hooks = new HashMap[DefaultKey, IHookFactoryUI] with SynchronizedMap[DefaultKey, IHookFactoryUI]

  val sources = new HashMap[DefaultKey, ISourceFactoryUI] with SynchronizedMap[DefaultKey, ISourceFactoryUI]

  val builders = new HashMap[NameKey, IBuilderFactoryUI] with SynchronizedMap[NameKey, IBuilderFactoryUI]

  val authentifications = new HashMap[DefaultKey, IAuthentificationFactoryUI] with SynchronizedMap[DefaultKey, IAuthentificationFactoryUI]

  val groupingStrategies = new HashMap[DefaultKey, IGroupingFactoryUI] with SynchronizedMap[DefaultKey, IGroupingFactoryUI]

  def task(c: Class[_]) = {
    val key = Key(c)
    if (tasks.contains(key)) tasks(key)
    else {
      val inter = intersection(c, tasks.keys.map { _.key }.toList)
      if (!inter.isEmpty) {
        val factory = tasks(Key(inter.head))
        tasks += key -> factory
        factory
      } else throw new UserBadDataError("The class " + c + " can not be constructed")
    }
  }

  def sampling(c: Class[_]) = {
    val key = Key(c)
    if (samplings.contains(key)) samplings(key)
    else {
      val inter = intersection(c, samplings.keys.map { _.key }.toList)
      if (!inter.isEmpty) {
        val factory = samplings(Key(inter.head))
        samplings += key -> factory
        factory
      } else throw new UserBadDataError("The class " + c + " can not be constructed")
    }
  }

  private def intersection(c: Class[_], lClass: List[Class[_]]) =
    lClass.intersect(c.listSuperClassesAndInterfaces.tail)

  def protoProxyKeyMap = Proxys.prototypes.map { p ⇒ KeyPrototypeGenerator(p) -> p }.toMap

  def samplingProxyKeyMap = Proxys.samplings.map { s ⇒ KeyGenerator(s.getClass) -> s }.toMap

  def environmentProxyKeyMap = Proxys.environments.map { e ⇒ KeyGenerator(e.getClass) -> e }.toMap
}
