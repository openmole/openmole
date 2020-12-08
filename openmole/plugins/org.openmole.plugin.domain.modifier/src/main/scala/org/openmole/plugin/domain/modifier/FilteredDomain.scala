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

object FilteredDomain {

  implicit def isDiscrete[D, I] = new DiscreteFromContext[FilteredDomain[D, I], I] with DomainInputs[FilteredDomain[D, I]] {
    override def iterator(domain: FilteredDomain[D, I]) = domain.iterator
    override def inputs(domain: FilteredDomain[D, I]) = domain.inputs
  }

}

case class FilteredDomain[D, I](domain: D, f: FromContext[I ⇒ Boolean])(implicit discrete: DiscreteFromContext[D, I], domainInputs: DomainInputs[D]) { d ⇒

  def inputs = domainInputs.inputs(domain)

  def iterator = FromContext { p ⇒
    import p._
    val fVal = f.from(context)
    discrete.iterator(domain).from(context).filter(fVal)
  }
}
