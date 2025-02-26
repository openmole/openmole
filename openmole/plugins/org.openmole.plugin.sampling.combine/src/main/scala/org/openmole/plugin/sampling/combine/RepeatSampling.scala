/*
 * Copyright (C) 2014 Romain Reuillon
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

object RepeatSampling {

  implicit def isSampling[S]: IsSampling[RepeatSampling[S]] = s => {
    def validate: Validate = s.sampling(s.s).validate ++ s.times.validate
    def inputs: PrototypeSet = s.sampling(s.s).inputs
    def outputs: Iterable[Val[?]] = s.sampling(s.s).outputs.map(_.toArray)
    def apply: FromContext[Iterator[Iterable[Variable[?]]]] = FromContext { p =>
      import p._

      def sampled =
        val samplingValue: Seq[Seq[Variable[?]]] = s.sampling(s.s).sampling.from(context).map(_.toSeq).toSeq.transpose

        for {
          vs ← samplingValue
        } yield {
          val p = vs.head.prototype
          Variable.unsecure(p.toArray, vs.map(_.value).toArray(p.`type`.manifest.asInstanceOf[Manifest[Any]]))
        }

      Iterator.continually(sampled).take(s.times.from(context))
    }

    Sampling(
      apply,
      outputs,
      inputs = inputs,
      validate = validate
    )
  }

}

case class RepeatSampling[S](s: S, times: FromContext[Int])(implicit val sampling: IsSampling[S])

