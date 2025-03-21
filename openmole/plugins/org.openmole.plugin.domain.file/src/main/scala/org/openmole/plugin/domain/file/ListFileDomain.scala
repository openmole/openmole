/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.domain.file

import java.io.File
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.argument.OptionalArgument
import org.openmole.plugin.domain.modifier.*

object ListFileDomain extends JavaLogger:

  given DiscreteFromContextDomain[ListFileDomain, File] = domain =>
    Domain(
      domain.iterator,
      domain.directory.toSeq.flatMap(_.inputs) ++ domain.filter.toSeq.flatMap(_.inputs),
      domain.directory.toSeq.map(_.validate) ++ domain.filter.toSeq.map(_.validate)
    )

  given DiscreteDomainModifiers[ListFileDomain] with {}

  def apply(
    base:      File,
    directory: OptionalArgument[FromContext[String]] = OptionalArgument(),
    recursive: Boolean                               = false,
    filter:    OptionalArgument[FromContext[String]] = OptionalArgument()
  ): ListFileDomain = new ListFileDomain(base, directory, recursive, filter)



class ListFileDomain(
  base:                  File,
  private val directory: Option[FromContext[String]] = None,
  recursive:             Boolean                     = false,
  private val filter:    Option[FromContext[String]] = None
):

  def iterator =
    FromContext: p =>
      import p._
      def toFilter(f: File) =
        filter.map(e => f.getName.matches(e.from(context))).getOrElse(true)

      val dir = directory.map(s => new File(base, s.from(context))).getOrElse(base)

      if !dir.exists
      then Iterator.empty
      else
        if recursive
        then dir.listRecursive(toFilter).iterator
        else dir.listFilesSafe(toFilter).iterator


