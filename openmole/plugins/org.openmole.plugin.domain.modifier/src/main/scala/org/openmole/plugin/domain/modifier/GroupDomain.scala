/*
 * Copyright (C) 2011 romain
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
import cats._
import cats.implicits._

object GroupDomain:

  implicit def isDiscrete[D, T: Manifest]: DiscreteFromContextDomain[GroupDomain[D, T], Array[T]] = domain =>
    import domain._
    def iterator = (discrete(d).domain map2 size)((it, s) => it.grouped(s) map (_.toArray))
    Domain(
      iterator,
      discrete(d).inputs ++ domain.size.inputs,
      discrete(d).validate ++ domain.size.validate
    )

  given [T, D]: DiscreteDomainModifiers[GroupDomain[T, D]] with {}

case class GroupDomain[D, T: Manifest](d: D, size: FromContext[Int])(implicit val discrete: DiscreteFromContextDomain[D, T])
