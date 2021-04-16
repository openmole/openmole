/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

object ZipWithIndexSampling {

  implicit def isSampling[S]: IsSampling[ZipWithIndexSampling[S]] = new IsSampling[ZipWithIndexSampling[S]] {
    override def validate(s: ZipWithIndexSampling[S]): Validate = s.sampling.validate(s.s)
    override def inputs(s: ZipWithIndexSampling[S]): PrototypeSet = s.sampling.inputs(s.s)
    override def outputs(s: ZipWithIndexSampling[S]): Iterable[Val[_]] = s.sampling.outputs(s.s) ++ Seq(s.index)
    override def apply(s: ZipWithIndexSampling[S]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
      import p._
      s.sampling(s.s).from(context).zipWithIndex.map {
        case (line, i) ⇒ line ++ List(Variable(s.index, i))
      }

    }
  }

}

case class ZipWithIndexSampling[S](s: S, index: Val[Int])(implicit val sampling: IsSampling[S])

