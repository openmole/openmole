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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object MapDomain {

  implicit def isDiscrete[D, I, O]: DiscreteFromContextDomain[MapDomain[D, I, O], O] = domain ⇒ domain.iterator
  implicit def inputs[D, I, O](implicit domainInputs: DomainInput[D]): DomainInput[MapDomain[D, I, O]] = domain ⇒ domainInputs(domain.domain) ++ domain.f.inputs
  implicit def validate[D, I, O](implicit validate: DomainValidation[D]): DomainValidation[MapDomain[D, I, O]] = domain ⇒ validate(domain.domain) ++ domain.f.validate

}

case class MapDomain[D, -I, +O](domain: D, f: FromContext[I ⇒ O])(implicit discrete: DiscreteFromContextDomain[D, I]) { d ⇒

  def iterator = FromContext { p ⇒
    import p._
    val fVal = f.from(context)
    discrete.iterator(domain).from(context).map { fVal }
  }
}
