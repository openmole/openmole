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
import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.model.sampling._
import org.openmole.misc.workspace._
import org.openmole.core.implementation.sampling._

package object combine {

  implicit def combineSamplingDecorator(s: Sampling) = new {
    def +(s2: Sampling) = new CombineSampling(s, s2)
    def x(s2: Sampling) = new CompleteSampling(s, s2)
    def zip(s2: Sampling) = new ZipSampling(s, s2)
    def zipWithIndex(index: Prototype[Int]) = new ZipWithIndexSampling(s, index)
    def take(n: Int) = new TakeSampling(s, n)
    def shuffle = new ShuffleSampling(s)
  }

  implicit def zipWithNameFactorDecorator(factor: Factor[File, Domain[File] with Discrete[File]]) = new {
    def zipWithName(name: Prototype[String]) = new ZipWithNameSampling(factor, name)
  }

  implicit def combineFactorDecorator[T, D <: Domain[T] with Discrete[T]](f: Factor[T, D]) = new {
    def x(s: Sampling) = new CompleteSampling(f, s)
  }

}