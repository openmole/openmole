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

object TakeDomain {
  implicit def isDiscrete[D, T]: DiscreteFromContextDomain[TakeDomain[D, T], T] = domain ⇒ FromContext { p ⇒
    import p._
    val s = domain.size.from(context)
    domain.discrete.
      iterator(domain.domain).
      from(context).
      slice(0, s)
  }

  implicit def inputs[D, T](implicit domainInputs: RequiredInput[D]): RequiredInput[TakeDomain[D, T]] = domain ⇒ domain.size.inputs ++ domainInputs.apply(domain.domain)
  implicit def validate[D, T](implicit validate: ExpectedValidation[D]): ExpectedValidation[TakeDomain[D, T]] = domain ⇒ domain.size.validate ++ validate(domain.domain)
}

case class TakeDomain[D, +T](domain: D, size: FromContext[Int])(implicit val discrete: DiscreteFromContextDomain[D, T])
