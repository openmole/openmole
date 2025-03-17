/**
 * Copyright (C) 2016 Jonathan Passerat-Palmbach
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

package org.openmole.plugin.domain.file

import java.nio.file.Path
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import cats.implicits.*
import org.openmole.core.argument.OptionalArgument
import org.openmole.plugin.domain.modifier.*

object ListPathDomain:

  given DiscreteFromContextDomain[ListPathDomain, Path] = domain =>
    Domain(
      domain.iterator,
      domain.directory.toSeq.flatMap(_.inputs) ++ domain.filter.toSeq.flatMap(_.inputs),
      domain.directory.toSeq.map(_.validate) ++ domain.filter.toSeq.map(_.validate)
    )

  given DiscreteDomainModifiers[ListPathDomain] with {}

  def apply(
    base:      File,
    directory: OptionalArgument[FromContext[String]] = OptionalArgument(),
    recursive: Boolean                               = false,
    filter:    OptionalArgument[FromContext[String]] = OptionalArgument()
  ): ListPathDomain = new ListPathDomain(base, directory, recursive, filter)



class ListPathDomain(
  base:                  File,
  private val directory: Option[FromContext[String]] = None,
  recursive:             Boolean                     = false,
  private val filter:    Option[FromContext[String]] = None
) {

  def iterator =
    new ListFileDomain(base, directory, recursive, filter).iterator.map(_.map(_.toPath))

}
