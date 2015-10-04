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
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.data._

import scala.util.Random

object SortByNameDomain {
  implicit def isFinite[D] = new Finite[File, SortByNameDomain[D]] {
    override def computeValues(domain: SortByNameDomain[D], context: Context)(implicit rng: RandomProvider): Iterable[File] =
      domain.computeValues(context)

    override def inputs(domain: SortByNameDomain[D]): PrototypeSet = domain.inputs
  }

  def apply[D](d: D)(implicit finite: Finite[File, D]) = new SortByNameDomain(d)
}

class SortByNameDomain[D](val domain: D)(implicit val finite: Finite[File, D]) {

  def inputs = finite.inputs(domain)

  def computeValues(context: Context)(implicit rng: RandomProvider): Iterable[File] = {
    def extractNumber(name: String) = {
      val n = name.reverse.dropWhile(!_.isDigit).takeWhile(_.isDigit).reverse
      if (n.isEmpty) throw new UserBadDataError("File name " + name + " doesn't contains a number")
      else n.toInt
    }
    finite.computeValues(domain, context).toList.sortBy(f ⇒ extractNumber(f.getName))
  }

}
