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

import java.io.File
import java.nio.file.Path

import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain.Finite
import org.openmole.core.workflow.dsl._
import cats.implicits._

object ListPathsDomain {

  implicit def isFinite = new Finite[ListPathsDomain, Path] {
    override def computeValues(domain: ListPathsDomain) = domain.computeValues
  }

  def apply(
    base:      File,
    directory: OptionalArgument[FromContext[String]] = OptionalArgument(),
    recursive: Boolean                               = false,
    filter:    OptionalArgument[FromContext[String]] = OptionalArgument()
  ): ListPathsDomain = new ListPathsDomain(base, directory, recursive, filter)

}

class ListPathsDomain(
  base:      File,
  directory: Option[FromContext[String]] = None,
  recursive: Boolean                     = false,
  filter:    Option[FromContext[String]] = None
) {

  def computeValues: FromContext[Iterable[Path]] =
    new ListFilesDomain(base, directory, recursive, filter).computeValues.map(_.map(_.toPath))

}
