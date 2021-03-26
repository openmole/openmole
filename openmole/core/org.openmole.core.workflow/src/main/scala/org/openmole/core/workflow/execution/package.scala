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
package org.openmole.core.workflow

import java.io.PrintStream

import org.openmole.core.workflow.mole.MoleServices

package object execution {

  def display(stream: PrintStream, label: String, content: String) =
    if (!content.isEmpty) {
      stream.synchronized {
        val fullLength = 40
        val dashes = fullLength - label.length / 2
        val header = ("-" * dashes) + label + ("-" * (dashes - (label.length % 2)))
        val footer = "-" * header.length
        stream.println(header)
        stream.print(content)
        stream.println(footer)
      }
    }

  object EnvironmentProvider {
    def apply(build: MoleServices ⇒ LocalEnvironment): LocalEnvironmentProvider = LocalEnvironmentProvider(build)
    def apply[T <: Environment](build: MoleServices ⇒ T): EnvironmentProvider = GenericEnvironmentProvider(build)

    def multiple(build: MoleServices ⇒ (Environment, Seq[(EnvironmentProvider, Environment)])) = MultipleEnvironmentProvider(build)

    def build(p: EnvironmentProvider, services: MoleServices) =
      p match {
        case GenericEnvironmentProvider(build) ⇒ Seq(p -> build(services))
        case LocalEnvironmentProvider(build)   ⇒ Seq(p -> build(services))
        case MultipleEnvironmentProvider(build) ⇒
          val (e1, environments) = build(services)
          Seq(p -> e1) ++ environments
      }

    def buildLocal(p: LocalEnvironmentProvider, services: MoleServices) = p.build(services)
  }

  sealed trait EnvironmentProvider
  case class GenericEnvironmentProvider(build: MoleServices ⇒ Environment) extends EnvironmentProvider
  case class LocalEnvironmentProvider(build: MoleServices ⇒ LocalEnvironment) extends EnvironmentProvider
  case class MultipleEnvironmentProvider(build: MoleServices ⇒ (Environment, Seq[(EnvironmentProvider, Environment)])) extends EnvironmentProvider

}
