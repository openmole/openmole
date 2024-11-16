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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.core.workflow.execution

import org.jasypt.encryption.pbe.config.EnvironmentPBEConfig

import java.io.PrintStream
import org.openmole.core.workflow.mole.MoleServices
import org.openmole.tool.cache.KeyValueCache


def display(stream: PrintStream, label: String, content: String) =
  if content.nonEmpty
  then
    stream.synchronized:
      val fullLength = 40
      val dashes = fullLength - label.length / 2
      val header = ("-" * dashes) + label + ("-" * (dashes - (label.length % 2)))
      val footer = "-" * header.length
      stream.println(header)
      stream.print(content)
      stream.println(footer)


object EnvironmentProvider:

  type Cache = Map[EnvironmentProvider, Environment]

  def apply(build: MoleServices => LocalEnvironment): LocalEnvironmentProvider = LocalEnvironmentProvider(build)

  def apply[T <: Environment](build: (MoleServices, KeyValueCache) => T): EnvironmentProvider = GenericEnvironmentProvider(build)

  def multiple(build: (MoleServices, KeyValueCache, Cache) => (Environment, Cache)) = MultipleEnvironmentProvider(build)

  def build(p: Seq[EnvironmentProvider], services: MoleServices, keyValueCache: KeyValueCache, cache: Cache = Map()): Cache =

    def build0(lp: List[EnvironmentProvider], cache: Cache): Cache =
      lp match
        case Nil => cache
        case h :: t =>
          cache.get(h) match
            case Some(e) => build0(t, cache)
            case None =>
              h match
                case GenericEnvironmentProvider(bp) => build0(t, cache + (h -> bp(services, keyValueCache)))
                case LocalEnvironmentProvider(bp)   => build0(t, cache + (h -> bp(services)))
                case MultipleEnvironmentProvider(bp) =>
                  val (e1, cache2) = bp(services, keyValueCache, cache)
                  build0(t, cache ++ cache2 + (h -> e1))


    build0(p.toList, cache)

  def buildLocal(p: LocalEnvironmentProvider, services: MoleServices) = p.build(services)



sealed trait EnvironmentProvider
case class GenericEnvironmentProvider(build: (MoleServices, KeyValueCache) => Environment) extends EnvironmentProvider
case class LocalEnvironmentProvider(build: MoleServices => LocalEnvironment) extends EnvironmentProvider
case class MultipleEnvironmentProvider(build: (MoleServices, KeyValueCache, EnvironmentProvider.Cache) => (Environment, EnvironmentProvider.Cache)) extends EnvironmentProvider

type ExecutionState = Byte


