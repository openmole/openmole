/*
 * Copyright (C) 2010 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.domain.modifier

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.tools._
import scala.reflect.runtime.universe._

import scala.util.Random
import scalaz._
import Scalaz._

object MapDomain {

  implicit def isDiscrete[D, I, O] = new Discrete[MapDomain[D, I, O], O] with DomainInputs[MapDomain[D, I, O]] {
    override def iterator(domain: MapDomain[D, I, O]) = FromContext((context, rng) ⇒ domain.iterator(context)(rng))
    override def inputs(domain: MapDomain[D, I, O]) = domain.inputs
  }

  def apply[D[_], I, O](domain: D[I], f: I ⇒ O)(implicit discrete: Discrete[D[I], I], domainInputs: DomainInputs[D[I]]): MapDomain[D[I], I, O] =
    new MapDomain[D[I], I, O](domain, f)

}

class MapDomain[D, -I, +O](domain: D, f: I ⇒ O)(implicit discrete: Discrete[D, I], domainInputs: DomainInputs[D]) { d ⇒

  def inputs = domainInputs.inputs(domain)

  def iterator(context: Context)(implicit rng: RandomProvider): Iterator[O] =
    discrete.iterator(domain).from(context).map { f }
}
