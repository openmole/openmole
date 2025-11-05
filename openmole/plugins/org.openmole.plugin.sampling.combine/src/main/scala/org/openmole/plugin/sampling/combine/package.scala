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

package org.openmole.plugin.sampling.combine

import org.openmole.core.context.Val
import org.openmole.core.argument.FromContext
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.domain.collection._
import org.openmole.plugin.domain.modifier._
import org.openmole.core.argument._


implicit class SamplingCombineDecorator[T](s: T)(implicit isSampling: IsSampling[T]):
  infix def shuffle = ShuffleSampling(s)
  infix def filter(keep: Condition) = FilteredSampling(s, keep)
  infix def take(n: FromContext[Int]) = TakeSampling(s, n)
  infix def subset(n: Int, size: FromContext[Int] = 100) = SubsetSampling(s, n, size = size)
  infix def drop(n: FromContext[Int]) = DropSampling(s, n)

  infix def x[S2: IsSampling](s2: S2) = XSampling(s, s2)
  infix def ++[S2: IsSampling](s2: S2) = ConcatenateSampling(s, s2)

  @deprecated("Use ++", "13")
  def ::[S2: IsSampling](s2: S2) = ConcatenateSampling(s, s2)

  infix def zip[S2: IsSampling](s2: S2) = ZipSampling(s, s2)

  @deprecated("Use withIndex", "5")
  infix def zipWithIndex(index: Val[Int]) = withIndex(index)
  infix def withIndex(index: Val[Int]) = ZipWithIndexSampling(s, index)
  infix def sample(n: FromContext[Int]) = SampleSampling(s, n)
  infix def repeat(n: FromContext[Int]) = RepeatSampling(s, n)
  infix def bootstrap(samples: FromContext[Int], number: FromContext[Int]) = s sample samples repeat number

implicit class WithNameFactorDecorator[D, T: CanGetName](factor: Factor[D, T])(implicit discrete: DiscreteFromContextDomain[D, T]):
  @deprecated("Use withName", "5")
  infix def zipWithName(name: Val[String]): ZipWithNameSampling[D, T] = withName(name)
  infix def withName(name: Val[String]): ZipWithNameSampling[D, T] = new ZipWithNameSampling(factor, name)

export TupledSampling.*
