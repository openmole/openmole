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

import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.dsl._
import org.openmole.tool.logger.JavaLogger

object ListFilesDomain extends JavaLogger {

  implicit def isFinite: Finite[ListFilesDomain, File] = new Finite[ListFilesDomain, File] {
    override def computeValues(domain: ListFilesDomain) = domain.computeValues
  }

  def apply(
    base:      File,
    directory: OptionalArgument[FromContext[String]] = OptionalArgument(),
    recursive: Boolean                               = false,
    filter:    OptionalArgument[FromContext[String]] = OptionalArgument()
  ): ListFilesDomain = new ListFilesDomain(base, directory, recursive, filter)

}

import org.openmole.plugin.domain.file.ListFilesDomain.Log._

class ListFilesDomain(
  base:      File,
  directory: Option[FromContext[String]] = None,
  recursive: Boolean                     = false,
  filter:    Option[FromContext[String]] = None
) {

  def computeValues = FromContext[Iterable[File]] { p ⇒
    import p._
    def toFilter(f: File) =
      filter.map(e ⇒ f.getName.matches(e.from(context))).getOrElse(true)

    val dir = directory.map(s ⇒ new File(base, s.from(context))).getOrElse(base)

    if (!dir.exists) {
      logger.warning("Directory " + dir + " in ListFilesDomain doesn't exists, returning an empty list of values.")
      Iterable.empty
    }
    else if (recursive) dir.listRecursive(toFilter _)
    else dir.listFilesSafe(toFilter _)
  }

}
