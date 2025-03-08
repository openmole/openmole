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

object ZipWithDomain {

  implicit def isDiscrete[D, I, O]: DiscreteFromContextDomain[ZipWithDomain[D, I, O], (I, O)] = domain =>
    Domain(
      domain.iterator,
      domain.inputs,
      domain.validate
    )

}

case class ZipWithDomain[D, I, O](domain: D, f: FromContext[I => O])(implicit discrete: DiscreteFromContextDomain[D, I]) {
  def iterator = FromContext { p =>
    import p._
    discrete(domain).domain.from(context).map { e => e → f.from(context).apply(e) }
  }

  def inputs = discrete(domain).inputs ++ f.inputs
  def validate = discrete(domain).validate ++ f.validate
}

