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

import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._

object ZipWithDomain {

  implicit def isDiscrete[D, I, O] = new DiscreteFromContext[ZipWithDomain[D, I, O], (I, O)] with DomainInputs[ZipWithDomain[D, I, O]] {
    override def iterator(domain: ZipWithDomain[D, I, O]) = domain.iterator
    override def inputs(domain: ZipWithDomain[D, I, O]) = domain.inputs
  }

}

case class ZipWithDomain[D, I, O](domain: D, f: I ⇒ O)(implicit discrete: DiscreteFromContext[D, I], domainInputs: DomainInputs[D]) { d ⇒

  def inputs = domainInputs.inputs(domain)

  def iterator = FromContext { p ⇒
    import p._
    discrete.iterator(domain).from(context).map { e ⇒ e → f(e) }
  }
}

