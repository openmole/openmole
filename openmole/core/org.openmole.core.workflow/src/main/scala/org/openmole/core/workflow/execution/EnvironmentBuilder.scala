package org.openmole.core.workflow.execution

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

import org.openmole.core.workflow.mole.MoleServices
import org.openmole.tool.cache.KeyValueCache

object EnvironmentBuilder:

  type Cache = Map[EnvironmentBuilder, Environment]

  def apply(build: MoleServices => LocalEnvironment): LocalEnvironmentBuilder = LocalEnvironmentBuilder(build)

  def apply[T <: Environment](build: (MoleServices, KeyValueCache) => T): EnvironmentBuilder = GenericEnvironmentBuilder(build)

  def multiple(build: (MoleServices, KeyValueCache, Cache) => (Environment, Cache)) = MultipleEnvironmentBuilder(build)

  def build(p: Seq[EnvironmentBuilder], services: MoleServices, keyValueCache: KeyValueCache, cache: Cache = Map()): Cache =

    def build0(lp: List[EnvironmentBuilder], cache: Cache): Cache =
      lp match
        case Nil => cache
        case h :: t =>
          cache.get(h) match
            case Some(e) => build0(t, cache)
            case None =>
              h match
                case GenericEnvironmentBuilder(bp) => build0(t, cache + (h -> bp(services, keyValueCache)))
                case LocalEnvironmentBuilder(bp)   => build0(t, cache + (h -> bp(services)))
                case MultipleEnvironmentBuilder(bp) =>
                  val (e1, cache2) = bp(services, keyValueCache, cache)
                  build0(t, cache ++ cache2 + (h -> e1))


    build0(p.toList, cache)

  def buildLocal(p: LocalEnvironmentBuilder, services: MoleServices) = p.build(services)



sealed trait EnvironmentBuilder
case class GenericEnvironmentBuilder(build: (MoleServices, KeyValueCache) => Environment) extends EnvironmentBuilder
case class LocalEnvironmentBuilder(build: MoleServices => LocalEnvironment) extends EnvironmentBuilder
case class MultipleEnvironmentBuilder(build: (MoleServices, KeyValueCache, EnvironmentBuilder.Cache) => (Environment, EnvironmentBuilder.Cache)) extends EnvironmentBuilder

