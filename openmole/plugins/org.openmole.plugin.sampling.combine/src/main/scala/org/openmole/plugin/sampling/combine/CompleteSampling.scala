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
import org.openmole.core.argument.{ FromContext, Validate }
import org.openmole.core.workflow.sampling._

object CompleteSampling {
  implicit def isSampling: IsSampling[CompleteSampling] = s ⇒
    Sampling(
      s.apply(),
      s.outputs,
      s.inputs,
      s.validate)

  def apply(samplings: Sampling*) = new CompleteSampling(samplings *)
}

case class CompleteSampling(samplings: Sampling*) {

  def validate: Validate = samplings.flatMap(_.validate)
  def inputs = PrototypeSet.empty ++ samplings.flatMap { _.inputs }
  def outputs: Iterable[Val[?]] = samplings.flatMap { _.outputs }

  def apply() = FromContext { p ⇒
    import p._
    if (samplings.isEmpty) Iterator.empty
    else
      samplings.tail.foldLeft[Iterator[Iterable[Variable[?]]]](samplings.head.sampling.from(context)) {
        (a, b) ⇒ combine(a, b).from(context)
      }
  }

  def combine(s1: Iterator[Iterable[Variable[?]]], s2: Sampling) = FromContext { p ⇒
    import p._
    for (x ← s1; y ← s2.sampling.from(context ++ x)) yield x ++ y
  }

}

object XSampling {

  implicit def isSampling[S1, S2]: IsSampling[XSampling[S1, S2]] = s ⇒ {
    def validate =
      s.sampling1(s.s1).validate ++ Validate { p ⇒
        import p._
        s.sampling2(s.s2).validate.apply(p.inputs ++ s.sampling1(s.s1).outputs)
      }

    def inputs = s.sampling1(s.s1).inputs ++ s.sampling2(s.s2).inputs
    def outputs = s.sampling1(s.s1).outputs ++ s.sampling2(s.s2).outputs
    def apply = FromContext { p ⇒
      import p._
      for {
        x ← s.sampling1(s.s1).sampling.from(context)
        y ← s.sampling2(s.s2).sampling.from(context ++ x)
      } yield x ++ y
    }

    Sampling(
      apply,
      outputs,
      inputs = inputs,
      validate = validate
    )
  }

}

case class XSampling[S1, S2](s1: S1, s2: S2)(implicit val sampling1: IsSampling[S1], val sampling2: IsSampling[S2])