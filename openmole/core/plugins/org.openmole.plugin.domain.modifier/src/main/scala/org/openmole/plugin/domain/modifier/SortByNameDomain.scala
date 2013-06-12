/*
 * Copyright (C) 19/12/12 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.domain.modifier

import java.io.File
import org.openmole.core.model.domain._
import org.openmole.core.model.data._
import org.openmole.misc.exception._

object SortByNameDomain {
  def apply(domain: Domain[File] with Finite[File]) = new SortByNameDomain(domain)
}

class SortByNameDomain(val domain: Domain[File] with Finite[File]) extends Domain[File] with Finite[File] {

  override def inputs = domain.inputs

  override def computeValues(context: Context): Iterable[File] = {
    def extractNumber(name: String) = {
      val n = name.reverse.dropWhile(!_.isDigit).takeWhile(_.isDigit).reverse
      if (n.isEmpty) throw new UserBadDataError("File name " + name + " doesn't contains a number")
      else n.toInt
    }
    domain.computeValues(context).toList.sortBy(f ⇒ extractNumber(f.getName))
  }

}
