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
package org.openmole.core.workflow.sampling

import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.workflow.domain._
import cats.implicits._

object FactorSampling {

  /**
   * Construct a sampling from a [[Factor]], given an implicit discrete domain and implicit optional inputs for the domain (as [[org.openmole.core.workflow.domain.DomainInputs]])
   * @param f the factor
   * @param discrete discrete domain
   * @param domainInputs  optional inputs of the domain
   * @return
   */
  def apply[D, T](f: Factor[D, T])(implicit discrete: DiscreteFromContext[D, T], domainInputs: DomainInputs[D] = DomainInputs.empty) =
    new Sampling {
      override def inputs = domainInputs.inputs(f.domain)
      override def prototypes = List(f.value)
      override def apply(): FromContext[Iterator[collection.Iterable[Variable[T]]]] =
        discrete.iterator(f.domain).map(_.map { v ⇒ List(Variable(f.value, v)) })
    }

}