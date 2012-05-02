/*
 * Copyright (C) 2010 reuillon
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
import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IDomain
import org.openmole.misc.tools.service.Logger
import scala.collection.JavaConversions._
import org.openmole.core.model.domain.IFinite
import org.openmole.misc.tools.io.FileUtil._

object ListFilesDomain extends Logger

sealed class ListFilesDomain(dir: File, filter: File ⇒ Boolean) extends IDomain[File] with IFinite[File] {

  import ListFilesDomain._

  def this(dir: File) = this(dir, f ⇒ true)
  def this(dir: File, pattern: String) = this(dir, _.getName.matches(pattern))
  def this(dir: String, pattern: String) = this(new File(dir), _.getName.matches(pattern))
  def this(dir: String) = this(new File(dir))

  override def computeValues(context: IContext): Iterable[File] = {
    val files = dir.listFiles(filter)

    if (files == null) {
      logger.warning("Directory " + dir + " in ListFilesDomain doesn't exists, returning an empty list of values.")
      Iterable.empty
    } else files
  }

}
