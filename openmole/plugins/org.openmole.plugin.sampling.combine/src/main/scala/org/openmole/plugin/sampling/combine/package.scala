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

import java.io.File
import java.nio.file.Path
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.tools.FromContext

package object combine {

  trait AbstractSamplingCombineDecorator {
    def s: Sampling
    @deprecated("Use x instead", "5")
    def +(s2: Sampling) = x(s2)
    def x(s2: Sampling) = new CompleteSampling(s, s2)
    def ::(s2: Sampling) = new ConcatenateSampling(s, s2)
    def filter(keep: Condition) = FilteredSampling(s, keep)
    def zip(s2: Sampling) = ZipSampling(s, s2)
    @deprecated("Use withIndex", "5")
    def zipWithIndex(index: Prototype[Int]) = withIndex(index)
    def withIndex(index: Prototype[Int]) = ZipWithIndexSampling(s, index)
    def take(n: FromContext[Int]) = TakeSampling(s, n)
    def shuffle = ShuffleSampling(s)
    def sample(n: FromContext[Int]) = SampleSampling(s, n)
    def repeat(n: FromContext[Int]) = RepeatSampling(s, n)
    def bootstrap(samples: FromContext[Int], number: FromContext[Int]) = s sample samples repeat number
  }

  implicit class SamplingCombineDecorator(val s: Sampling) extends AbstractSamplingCombineDecorator

  implicit class DiscreteFactorDecorator[D, T](f: Factor[D, T])(implicit discrete: Discrete[D, T]) extends AbstractSamplingCombineDecorator {
    def s: Sampling = f
  }

  trait CanGetName[A] {
    def getName(a: A): String
  }

  implicit val fileGetName = new CanGetName[File] { def getName(f: File) = f.getName }
  implicit val pathGetName = new CanGetName[Path] { def getName(p: Path) = p.toFile.getName }

  implicit def withNameFactorDecorator[D, T: CanGetName](factor: Factor[D, T])(implicit discrete: Discrete[D, T]) = new {
    @deprecated("Use withName", "5")
    def zipWithName(name: Prototype[String]): ZipWithNameSampling[D, T] = withName(name)
    def withName(name: Prototype[String]): ZipWithNameSampling[D, T] = new ZipWithNameSampling(factor, name)
  }

}