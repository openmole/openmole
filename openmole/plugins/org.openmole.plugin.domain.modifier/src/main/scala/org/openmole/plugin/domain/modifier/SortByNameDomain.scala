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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object SortByNameDomain {

  implicit def isDiscrete[D]: DiscreteFromContextDomain[SortByNameDomain[D], File] = domain =>
    Domain(
      domain.iterator,
      domain.inputs,
      domain.validate
    )

}

case class SortByNameDomain[D](domain: D)(implicit val discrete: DiscreteFromContextDomain[D, File]) {
  def iterator =
    FromContext { p =>
      import p._
      def extractNumber(name: String) = {
        val n = name.reverse.dropWhile(!_.isDigit).takeWhile(_.isDigit).reverse
        if (n.isEmpty) throw new UserBadDataError("File name " + name + " doesn't contains a number")
        else n.toInt
      }

      discrete(domain).domain.from(context).toSeq.sortBy(f => extractNumber(f.getName)).iterator
    }

  def inputs = discrete(domain).inputs
  def validate = discrete(domain).validate
}
