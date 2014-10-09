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

import org.openmole.misc.tools.obj.ClassUtils.ClassDecorator
import org.openmole.ide.core.implementation.dataproxy.Proxies
import scala.collection.JavaConverters._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.implementation.factory._
import java.util.concurrent.ConcurrentLinkedQueue

import java.util.Collections
import java.util.LinkedList
import scala.collection.mutable.Buffer

object KeyRegistry {
  val prototypes = Collections.synchronizedList(new LinkedList[PrototypeFactoryUI]).asScala

  val tasks = Collections.synchronizedList(new LinkedList[TaskFactoryUI]).asScala

  val samplings = Collections.synchronizedList(new LinkedList[SamplingFactoryUI]).asScala

  val environments = Collections.synchronizedList(new LinkedList[EnvironmentFactoryUI]).asScala

  val domains = Collections.synchronizedList(new LinkedList[IDomainFactoryUI]).asScala

  val hooks = Collections.synchronizedList(new LinkedList[HookFactoryUI]).asScala

  val sources = Collections.synchronizedList(new LinkedList[SourceFactoryUI]).asScala

  val authentifications = Collections.synchronizedList(new LinkedList[AuthentificationFactoryUI]).asScala

  val groupingStrategies = Collections.synchronizedList(new LinkedList[GroupingFactoryUI]).asScala

  /* def task(c: Class[_]) = {
    val key = Key(c)
    if (tasks.contains(key)) tasks(key)
    else {
      val inter = intersection(c, tasks.keys.map { _.key }.toList)
      if (!inter.isEmpty) {
        val factory = tasks(Key(inter.head))
        tasks += key -> factory
        factory
      }
      else throw new UserBadDataError("The class " + c + " can not be constructed")
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
      }
      else throw new UserBadDataError("The class " + c + " can not be constructed")
    }
  }

  private def intersection(c: Class[_], lClass: List[Class[_]]) =
    lClass.intersect(c.listSuperClassesAndInterfaces.tail)*/

  def protoProxyKeyMap = Proxies.instance.prototypes.map { p ⇒ PrototypeKey(p) -> p }.toMap

  def samplingProxyKeyMap = Proxies.instance.samplings.map { s ⇒ KeyGenerator(s.getClass) -> s }.toMap

  def environmentProxyKeyMap = Proxies.instance.environments.map { e ⇒ KeyGenerator(e.getClass) -> e }.toMap
}
