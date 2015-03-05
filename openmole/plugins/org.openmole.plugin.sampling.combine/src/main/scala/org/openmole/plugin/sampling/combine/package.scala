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
import java.util.Random
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools.FromContext

package object combine {

  implicit def stringToGroovyFilterConversion(s: String) = new GroovyFilter(s)

  trait AbstractSamplingDecorator {
    def s: Sampling
    def +(s2: Sampling) = new CombineSampling(s, s2)
    def x(s2: Sampling) = new CompleteSampling(s, s2)
    def filter(filters: Filter*) = FilteredSampling(s, filters: _*)
    def zip(s2: Sampling) = ZipSampling(s, s2)
    def zipWithIndex(index: Prototype[Int]) = ZipWithIndexSampling(s, index)
    def take(n: FromContext[Int]) = TakeSampling(s, n)
    def shuffle = ShuffleSampling(s)
    def sample(n: FromContext[Int]) = SampleSampling(s, n)
    def repeat(n: FromContext[Int]) = RepeatSampling(s, n)
    def bootstrap(samples: FromContext[Int], number: FromContext[Int]) = s sample samples repeat number
  }

  implicit class SamplingDecorator(val s: Sampling) extends AbstractSamplingDecorator
  implicit class DiscreteFactorDecorator[T, D <: Domain[T] with Discrete[T]](f: Factor[T, D]) extends AbstractSamplingDecorator {
    def s: Sampling = f
  }

  implicit def zipWithNameFactorDecorator(factor: Factor[File, Domain[File] with Discrete[File]]) = new {
    def zipWithName(name: Prototype[String]) = ZipWithNameSampling(factor, name)
  }

}