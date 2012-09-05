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
import org.openmole.core.implementation.tools._
import org.openmole.core.model.data.Context
import org.openmole.core.model.domain.IDomain
import org.openmole.misc.tools.service.Logger
import scala.collection.JavaConversions._
import org.openmole.core.model.domain.IFinite
import org.openmole.misc.tools.io.FileUtil._

object ListFilesDomain extends Logger

sealed class ListFilesDomain(base: File, subdirectory: String = "", filter: File ⇒ Boolean = f ⇒ true) extends IDomain[File] with IFinite[File] {

  override def computeValues(context: Context): Iterable[File] = {
    val dir = new File(base, VariableExpansion(context, subdirectory))

    Option(dir.listFiles(filter)) match {
      case Some(f) ⇒ f
      case None ⇒
        ListFilesDomain.logger.warning("Directory " + dir + " in ListFilesDomain doesn't exists, returning an empty list of values.")
        Iterable.empty
    }
  }

}
