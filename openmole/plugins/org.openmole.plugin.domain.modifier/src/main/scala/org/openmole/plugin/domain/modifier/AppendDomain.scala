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

  given [D1, D2, T]: DiscreteFromContextDomain[AppendDomain[D1, D2, T], T] =
    domain =>
      val d1Value = domain.domain1(domain.d1)
      val d2Value = domain.domain2(domain.d2)

      Domain(
        FromContext { p =>
          import p._
          d1Value.domain.from(context).iterator ++ d2Value.domain.from(context).iterator
        },
        d1Value.inputs ++ d2Value.inputs,
        d1Value.validation ++ d2Value.validation
      )


  given [T, D, A]: DiscreteDomainModifiers[AppendDomain[D, T, A]] with {}

}

case class AppendDomain[D1, D2, T](d1: D1, d2: D2)(
  implicit
  val domain1: DiscreteFromContextDomain[D1, T],
  val domain2: DiscreteFromContextDomain[D2, T])