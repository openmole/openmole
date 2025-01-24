/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.workflow.sampling


/**
 * Sampling aims at associating prototypes with values.
 */

import org.openmole.core.context._
import org.openmole.core.argument._
import org.openmole.tool.types._
import org.openmole.core.workflow.domain._
import cats.implicits._
import org.openmole.core.tools.io.Prettifier

trait SamplingPackage {

  implicit class PrototypeFactorDecorator[T](p: Val[T]) {
    def is(d: FromContext[T]) = Factor(p, d)
  }

  implicit def fromContextIsDiscrete[T]: DiscreteFromContextDomain[FromContext[T], T] = domain => Domain(domain.map(v => Vector(v).iterator))
  implicit def fromContextIterableIsDiscrete[T]: DiscreteFromContextDomain[FromContext[Iterable[T]], T] = domain => Domain(domain.map(v => v.iterator))

  implicit def factorIsSampling[D, T](implicit domain: DiscreteFromContextDomain[D, T]): IsSampling[Factor[D, T]] = f => {
    def inputs = {
      val domainValue = domain(f.domain)
      domain(f.domain).inputs ++ domainValue.inputs
    }

    def outputs = List(f.value)

    def validate: Validate = {
      val domainValue = domain(f.domain)
      domainValue.domain.validate ++ domainValue.validation
    }

    Sampling(
      domain(f.domain).domain.map { values => values.map { v => List(Variable(f.value, v)) } },
      outputs,
      inputs,
      validate,
    )
  }

  type Sampling = org.openmole.core.workflow.sampling.Sampling

  object EmptySampling {
    implicit def isSampling: IsSampling[EmptySampling] = s =>
      Sampling(
        FromContext.value(Iterator.empty),
        Seq(),
        Seq(),
        Validate.success
      )
  }

  case class EmptySampling()
}


import org.openmole.core.context._
import org.openmole.core.keyword._

/**
 * The factor type associates a Val to a domain through the keyword In
 * @tparam D
 * @tparam T
 */
type Factor[D, T] = In[Val[T], D]

/**
 * Construct a [[Factor]] from a prototype and its domain
 * @param p
 * @param d
 * @return
 */
def Factor[D, T](p: Val[T], d: D) = In(p, d)

