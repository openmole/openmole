/**
 * Created by Romain Reuillon on 03/06/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.plugin.domain.modifier

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object ZipWithIndexDomain {
  implicit def isDiscrete[D, I]: DiscreteFromContextDomain[ZipWithIndexDomain[D, I], (I, Int)] = domain =>
    Domain(
      domain.iterator,
      domain.inputs,
      domain.validate
    )

  given [T, D]: DiscreteDomainModifiers[ZipWithIndexDomain[D, T]] with {}
}

case class ZipWithIndexDomain[D, I](domain: D)(implicit discrete: DiscreteFromContextDomain[D, I]) { d =>

  def iterator = FromContext { p =>
    import p._
    discrete(domain).domain.from(context).zipWithIndex
  }

  def inputs = discrete(domain).inputs
  def validate = discrete(domain).validate

}