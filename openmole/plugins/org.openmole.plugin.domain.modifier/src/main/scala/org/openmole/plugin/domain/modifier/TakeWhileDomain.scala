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
import cats._
import cats.implicits._

object TakeWhileDomain {

  implicit def isFinite[D, T]: DiscreteFromContextDomain[TakeWhileDomain[D, T], T] = domain =>
    Domain(
      domain.iterator,
      domain.inputs,
      domain.validate
    )

}

case class TakeWhileDomain[D, T](domain: D, predicate: FromContext[T => Boolean])(implicit discrete: DiscreteFromContextDomain[D, T]) {
  def iterator =
    (discrete(domain).domain map2 predicate)((d, p) => d.takeWhile(p))

  def inputs = discrete(domain).inputs ++ predicate.inputs
  def validate = discrete(domain).validate ++ predicate.validate

}
