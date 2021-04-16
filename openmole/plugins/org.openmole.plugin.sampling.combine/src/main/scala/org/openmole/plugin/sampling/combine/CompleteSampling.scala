/*
 * Copyright (C) 2012 Romain Reuillon
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

import org.openmole.core.context.{ Context, PrototypeSet, Val, Variable }
import org.openmole.core.expansion.{ FromContext, Validate }
import org.openmole.core.workflow.sampling._

object CompleteSampling {

  implicit def isSampling: IsSampling[CompleteSampling] = new IsSampling[CompleteSampling] {
    override def validate(s: CompleteSampling, inputs: Seq[Val[_]]): Validate = s.samplings.flatMap(_.validate(inputs))
    override def inputs(s: CompleteSampling): PrototypeSet = s.samplings.flatMap { _.inputs }
    override def outputs(s: CompleteSampling): Iterable[Val[_]] = s.samplings.flatMap { _.outputs }

    override def apply(s: CompleteSampling) = FromContext { p ⇒
      import p._
      if (s.samplings.isEmpty) Iterator.empty
      else
        s.samplings.tail.foldLeft(s.samplings.head().from(context)) {
          (a, b) ⇒ combine(a, b).from(context)
        }
    }

    def combine(s1: Iterator[Iterable[Variable[_]]], s2: Sampling) = FromContext { p ⇒
      import p._
      for (x ← s1; y ← s2().from(context ++ x)) yield x ++ y
    }
  }

}

case class CompleteSampling(samplings: Sampling*)

object XSampling {

  implicit def isSampling[S1, S2]: IsSampling[XSampling[S1, S2]] = new IsSampling[XSampling[S1, S2]] {
    override def validate(s: XSampling[S1, S2], inputs: Seq[Val[_]]): Validate =
      s.sampling1.validate(s.s1, inputs) ++ s.sampling2.validate(s.s2, inputs ++ s.sampling1.outputs(s.s1))

    override def inputs(s: XSampling[S1, S2]): PrototypeSet = s.sampling1.inputs(s.s1) ++ s.sampling2.inputs(s.s2)
    override def outputs(s: XSampling[S1, S2]): Iterable[Val[_]] = s.sampling1.outputs(s.s1) ++ s.sampling2.outputs(s.s2)
    override def apply(s: XSampling[S1, S2]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
      import p._
      for {
        x ← s.sampling1.apply(s.s1).apply(context)
        y ← s.sampling2.apply(s.s2).apply(context ++ x)
      } yield x ++ y
    }
  }

}

case class XSampling[S1, S2](s1: S1, s2: S2)(implicit val sampling1: IsSampling[S1], val sampling2: IsSampling[S2])