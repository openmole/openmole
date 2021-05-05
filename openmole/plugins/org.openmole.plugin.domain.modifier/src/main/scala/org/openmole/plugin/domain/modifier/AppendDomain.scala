package org.openmole.plugin.domain.modifier

/*
 * Copyright (C) 2021 Romain Reuillon
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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object AppendDomain {

  implicit def isDiscrete[D1, D2, T]: DiscreteFromContextDomain[AppendDomain[D1, D2, T], T] =
    domain ⇒
      FromContext { p ⇒
        import p._
        domain.domain1.iterator(domain.d1).from(context).iterator ++
          domain.domain2.iterator(domain.d2).from(context).iterator
      }

  implicit def inputs[D1, D2, T](implicit d1Inputs: RequiredInput[D1], d2Inputs: RequiredInput[D2]): RequiredInput[AppendDomain[D1, D2, T]] = domain ⇒ d1Inputs(domain.d1) ++ d2Inputs(domain.d2)
  implicit def validate[D1, D2, T](implicit d1Validate: ExpectedValidation[D1], d2Validate: ExpectedValidation[D2]): ExpectedValidation[AppendDomain[D1, D2, T]] = domain ⇒ d1Validate(domain.d1) ++ d2Validate(domain.d2)

}

case class AppendDomain[D1, D2, T](d1: D1, d2: D2)(
  implicit
  val domain1: DiscreteFromContextDomain[D1, T],
  val domain2: DiscreteFromContextDomain[D2, T])