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

  implicit def isSampling[S]: IsSampling[RepeatSampling[S]] = new IsSampling[RepeatSampling[S]] {
    override def validate(s: RepeatSampling[S], inputs: Seq[Val[_]]): Validate = s.sampling.validate(s.s, inputs) ++ s.times.validate(inputs)
    override def inputs(s: RepeatSampling[S]): PrototypeSet = s.sampling.inputs(s.s)
    override def outputs(s: RepeatSampling[S]): Iterable[Val[_]] = s.sampling.outputs(s.s).map(_.toArray)
    override def apply(s: RepeatSampling[S]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
      import p._

      def sampled =
        for {
          vs ← s.sampling.apply(s.s).from(context).map(_.toSeq).toSeq.transpose
        } yield {
          val p = vs.head.prototype
          Variable.unsecure(p.toArray, vs.map(_.value).toArray(p.`type`.manifest.asInstanceOf[Manifest[Any]]))
        }

      Iterator.continually(sampled).take(s.times.from(context))
    }
  }

}

case class RepeatSampling[S](s: S, times: FromContext[Int])(implicit val sampling: IsSampling[S])

