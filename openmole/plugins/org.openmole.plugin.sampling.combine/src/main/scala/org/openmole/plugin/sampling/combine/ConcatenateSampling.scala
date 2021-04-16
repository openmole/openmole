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

  implicit def isSampling[S1, S2]: IsSampling[ConcatenateSampling[S1, S2]] = new IsSampling[ConcatenateSampling[S1, S2]] {
    override def validate(s: ConcatenateSampling[S1, S2], inputs: Seq[Val[_]]): Validate = s.sampling1.validate(s.s1, inputs) ++ s.sampling2.validate(s.s2, inputs)
    override def inputs(s: ConcatenateSampling[S1, S2]): PrototypeSet = s.sampling1.inputs(s.s1) ++ s.sampling2.inputs(s.s2)
    override def outputs(s: ConcatenateSampling[S1, S2]): Iterable[Val[_]] = {
      val p1 = s.sampling1.outputs(s.s1).toSet
      val p2 = s.sampling2.outputs(s.s2).toSet
      p1 intersect p2
    }
    override def apply(s: ConcatenateSampling[S1, S2]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
      import p._
      val ps = outputs(s).toSet

      s.sampling1.apply(s.s1).apply(context).map(_.filter(v ⇒ ps.contains(v.prototype))) ++
        s.sampling2.apply(s.s2).apply(context).map(_.filter(v ⇒ ps.contains(v.prototype)))
    }
  }

}

case class ConcatenateSampling[S1, S2](s1: S1, s2: S2)(implicit val sampling1: IsSampling[S1], val sampling2: IsSampling[S2])

