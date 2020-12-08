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

import org.openmole.core.context.PrototypeSet
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.domain._

import cats._
import cats.implicits._

object SortByNameDomain {
  implicit def isFinite[D] = new FiniteFromContext[SortByNameDomain[D], File] with DomainInputs[SortByNameDomain[D]] {
    override def computeValues(domain: SortByNameDomain[D]) = domain.computeValues()
    override def inputs(domain: SortByNameDomain[D]): PrototypeSet = domain.inputs
  }

}

case class SortByNameDomain[D](domain: D)(implicit val finite: FiniteFromContext[D, File], domainInputs: DomainInputs[D]) {

  def inputs = domainInputs.inputs(domain)

  def computeValues() = {
    def extractNumber(name: String) = {
      val n = name.reverse.dropWhile(!_.isDigit).takeWhile(_.isDigit).reverse
      if (n.isEmpty) throw new UserBadDataError("File name " + name + " doesn't contains a number")
      else n.toInt
    }

    for {
      f ← finite.computeValues(domain)
    } yield f.toList.sortBy(f ⇒ extractNumber(f.getName))
  }

}
