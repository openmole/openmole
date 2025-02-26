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
package org.openmole.plugin.sampling.combine

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object ConcatenateSampling {

  implicit def isSampling[S1, S2]: IsSampling[ConcatenateSampling[S1, S2]] = s => {
    def validate: Validate = s.sampling1(s.s1).validate ++ s.sampling2(s.s2).validate
    def inputs = s.sampling1(s.s1).inputs ++ s.sampling2(s.s2).inputs
    val outputs = {
      val p1 = s.sampling1(s.s1).outputs.toSet
      val p2 = s.sampling2(s.s2).outputs.toSet
      p1 intersect p2
    }
    def apply = FromContext { p =>
      import p._
      val ps = outputs.toSet

      s.sampling1(s.s1).sampling.from(context).map(_.filter(v => ps.contains(v.prototype))) ++
        s.sampling2(s.s2).sampling.from(context).map(_.filter(v => ps.contains(v.prototype)))
    }

    Sampling(
      apply,
      outputs,
      inputs = inputs,
      validate = validate
    )
  }

}

case class ConcatenateSampling[S1, S2](s1: S1, s2: S2)(implicit val sampling1: IsSampling[S1], val sampling2: IsSampling[S2])

