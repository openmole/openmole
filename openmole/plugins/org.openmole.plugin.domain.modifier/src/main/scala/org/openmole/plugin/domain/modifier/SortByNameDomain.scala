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
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.domain.{ DiscreteFromContext, DomainInputs }

object SortByNameDomain {

  implicit def isDiscrete[D] = new DiscreteFromContext[SortByNameDomain[D], File] with DomainInputs[SortByNameDomain[D]] {

    override def inputs(domain: SortByNameDomain[D]): PrototypeSet = domain.domainInputs.inputs(domain.domain)

    override def iterator(domain: SortByNameDomain[D]) = FromContext { p ⇒
      import p._
      def extractNumber(name: String) = {
        val n = name.reverse.dropWhile(!_.isDigit).takeWhile(_.isDigit).reverse
        if (n.isEmpty) throw new UserBadDataError("File name " + name + " doesn't contains a number")
        else n.toInt
      }

      domain.discrete.iterator(domain.domain).from(context).toSeq.sortBy(f ⇒ extractNumber(f.getName)).iterator
    }
  }

}

case class SortByNameDomain[D](domain: D)(implicit val discrete: DiscreteFromContext[D, File], val domainInputs: DomainInputs[D]) {

}
