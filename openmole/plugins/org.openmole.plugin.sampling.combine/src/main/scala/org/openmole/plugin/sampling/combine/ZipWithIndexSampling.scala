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

  implicit def isSampling[S]: IsSampling[ZipWithIndexSampling[S]] = s => {
    def validate: Validate = s.sampling(s.s).validate
    def inputs: PrototypeSet = s.sampling(s.s).inputs
    def outputs: Iterable[Val[?]] = s.sampling(s.s).outputs ++ Seq(s.index)
    def apply: FromContext[Iterator[Iterable[Variable[?]]]] = FromContext { p =>
      import p._
      s.sampling(s.s).sampling.from(context).zipWithIndex.map {
        case (line, i) => line ++ List(Variable(s.index, i))
      }
    }

    Sampling(
      apply,
      outputs,
      inputs = inputs,
      validate = validate
    )
  }

}

case class ZipWithIndexSampling[S](s: S, index: Val[Int])(implicit val sampling: IsSampling[S])

