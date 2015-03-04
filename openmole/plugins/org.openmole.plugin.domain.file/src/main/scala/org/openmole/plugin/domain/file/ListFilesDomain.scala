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
import org.openmole.core.tools.io.FileUtil
import org.openmole.core.tools.service.Logger
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.tools.ExpandedString
import scala.collection.JavaConversions._
import FileUtil._

import scala.util.Random

object ListFilesDomain extends Logger {

  def apply(
    base: File,
    subdirectory: ExpandedString = "",
    recursive: Boolean = false,
    filter: File ⇒ Boolean = f ⇒ true) = new ListFilesDomain(base, subdirectory, recursive, filter)

}

import ListFilesDomain.Log._

sealed class ListFilesDomain(
    base: File,
    subdirectory: ExpandedString = "",
    recursive: Boolean = false,
    filter: File ⇒ Boolean = f ⇒ true) extends Domain[File] with Finite[File] {

  override def computeValues(context: Context)(implicit rng: Random): Iterable[File] = {
    val dir = new File(base, subdirectory.from(context))

    if (!dir.exists) {
      logger.warning("Directory " + dir + " in ListFilesDomain doesn't exists, returning an empty list of values.")
      Iterable.empty
    }
    else if (recursive) dir.listRecursive(filter)
    else dir.listFiles(filter)
  }

}
