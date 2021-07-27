/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.sampling

import org.openmole.core.context.Val
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.domain.collection._
import org.openmole.plugin.domain.modifier._
import org.openmole.core.expansion._
import cats.implicits._

package object combine {

  implicit class SamplingCombineDecorator[T](s: T)(implicit isSampling: IsSampling[T]) {
    def shuffle = ShuffleSampling(s)
    def filter(keep: Condition) = FilteredSampling(s, keep)
    def take(n: FromContext[Int]) = TakeSampling(s, n)
    def subset(n: Int, size: FromContext[Int] = 100) = SubsetSampling(s, n, size = size)
    def drop(n: FromContext[Int]) = DropSampling(s, n)

    def x[S2: IsSampling](s2: S2) = XSampling(s, s2)
    def ++[S2: IsSampling](s2: S2) = ConcatenateSampling(s, s2)

    @deprecated("Use ++", "13")
    def ::[S2: IsSampling](s2: S2) = ConcatenateSampling(s, s2)

    def zip[S2: IsSampling](s2: S2) = ZipSampling(s, s2)

    @deprecated("Use withIndex", "5")
    def zipWithIndex(index: Val[Int]) = withIndex(index)
    def withIndex(index: Val[Int]) = ZipWithIndexSampling(s, index)
    def sample(n: FromContext[Int]) = SampleSampling(s, n)
    def repeat(n: FromContext[Int]) = RepeatSampling(s, n)
    def bootstrap(samples: FromContext[Int], number: FromContext[Int]) = s sample samples repeat number
  }

  implicit def withNameFactorDecorator[D, T: CanGetName](factor: Factor[D, T])(implicit discrete: DiscreteFromContextDomain[D, T]) = new {
    @deprecated("Use withName", "5")
    def zipWithName(name: Val[String]): ZipWithNameSampling[D, T] = withName(name)
    def withName(name: Val[String]): ZipWithNameSampling[D, T] = new ZipWithNameSampling(factor, name)
  }

  implicit class TupleToZipSampling[T1, T2](ps: (Val[T1], Val[T2])) {
    def in[D](d: D)(implicit discrete: DiscreteFromContextDomain[D, (T1, T2)]) = {
      val d1 = discrete(d).domain.map(_.map(_._1))
      val d2 = discrete(d).domain.map(_.map(_._2))
      ZipSampling(ps._1 in d1, ps._2 in d2)
    }
  }

}