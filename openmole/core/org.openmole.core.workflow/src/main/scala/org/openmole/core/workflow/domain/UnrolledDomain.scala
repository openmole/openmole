/*
 * Copyright (C) 2015 Romain Reuillon
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
 */
package org.openmole.core.workflow.domain

import org.openmole.core.context._
import org.openmole.core.expansion._
import cats.implicits._
import org.openmole.core.workflow.validation.{ ExpectedValidation, RequiredInput }

object UnrolledDomain {

  implicit def isDiscrete[D, T: Manifest]: DiscreteDomain[UnrolledDomain[D, T], Array[T]] = domain ⇒ Seq(domain.discrete.iterator(domain.d).toArray).iterator
  implicit def inputs[D, T: Manifest]: RequiredInput[UnrolledDomain[D, T]] = domain ⇒ domain.inputs(domain.d)
  implicit def validate[D, T: Manifest]: ExpectedValidation[UnrolledDomain[D, T]] = domain ⇒ domain.validate(domain.d)

  def apply[D[_], T: Manifest](domain: D[T])(implicit discrete: DiscreteDomain[D[T], T], inputs: RequiredInput[D[T]], validate: ExpectedValidation[D[T]]) =
    new UnrolledDomain[D[T], T](domain)

}

class UnrolledDomain[D, T: Manifest](val d: D)(implicit val discrete: DiscreteDomain[D, T], val inputs: RequiredInput[D], val validate: ExpectedValidation[D])
