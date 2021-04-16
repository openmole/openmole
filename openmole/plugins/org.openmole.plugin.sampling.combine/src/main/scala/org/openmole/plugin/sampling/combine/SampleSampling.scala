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

object SampleSampling {

  implicit def isSampling[S]: IsSampling[SampleSampling[S]] = new IsSampling[SampleSampling[S]] {
    override def validate(s: SampleSampling[S], inputs: Seq[Val[_]]): Validate = s.sampling.validate(s.s, inputs) ++ s.size.validate(inputs)
    override def inputs(s: SampleSampling[S]): PrototypeSet = s.sampling.inputs(s.s)
    override def outputs(s: SampleSampling[S]): Iterable[Val[_]] = s.sampling.outputs(s.s)
    override def apply(s: SampleSampling[S]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
      import p._
      val sampled = s.sampling(s.s).from(context).toVector
      val sampledSize = sampled.size
      val sizeValue = s.size.from(context)
      Iterator.continually(random().nextInt(sampledSize)).take(sizeValue).map(i ⇒ sampled(i))
    }
  }

}

case class SampleSampling[S](s: S, size: FromContext[Int])(implicit val sampling: IsSampling[S])