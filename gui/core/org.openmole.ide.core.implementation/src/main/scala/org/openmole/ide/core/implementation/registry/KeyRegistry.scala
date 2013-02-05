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
import org.openmole.core.model.domain.Domain
import org.openmole.ide.core.implementation.dataproxy.Proxys
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap

object KeyRegistry {
  val prototypes = new HashMap[DefaultKey, IPrototypeFactoryUI] with SynchronizedMap[DefaultKey, IPrototypeFactoryUI]

  val tasks = new HashMap[DefaultKey, ITaskFactoryUI] with SynchronizedMap[DefaultKey, ITaskFactoryUI]

  val samplings = new HashMap[DefaultKey, ISamplingFactoryUI] with SynchronizedMap[DefaultKey, ISamplingFactoryUI]

  val environments = new HashMap[DefaultKey, IEnvironmentFactoryUI] with SynchronizedMap[DefaultKey, IEnvironmentFactoryUI]

  val domains = new HashMap[DefaultKey, IDomainFactoryUI] with SynchronizedMap[DefaultKey, IDomainFactoryUI]

  val hooks = new HashMap[DefaultKey, IHookFactoryUI] with SynchronizedMap[DefaultKey, IHookFactoryUI]

  val builders = new HashMap[NameKey, IBuilderFactoryUI] with SynchronizedMap[NameKey, IBuilderFactoryUI]

  val authentifications = new HashMap[DefaultKey, IAuthentificationFactoryUI] with SynchronizedMap[DefaultKey, IAuthentificationFactoryUI]

  val groupingStrategies = new HashMap[DefaultKey, IGroupingFactoryUI] with SynchronizedMap[DefaultKey, IGroupingFactoryUI]

  def protoProxyKeyMap = Proxys.prototypes.map { p ⇒ KeyPrototypeGenerator(p) -> p }.toMap

  def samplingProxyKeyMap = Proxys.samplings.map { s ⇒ KeyGenerator(s.getClass) -> s }.toMap

  def environmentProxyKeyMap = Proxys.environments.map { e ⇒ KeyGenerator(e.getClass) -> e }.toMap
}
