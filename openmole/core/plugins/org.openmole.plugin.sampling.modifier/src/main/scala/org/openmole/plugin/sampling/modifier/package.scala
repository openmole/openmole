/*
 * Copyright (C) 2010 Romain Reuillon
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

import org.openmole.core.model.sampling._
import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import java.io.File

package object modifier {

  implicit def stringToGroovyFilterConversion(s: String) = new GroovyFilter(s)
  implicit def modifierSamplingDecorator(s: Sampling) = new {
    def filter(filters: Filter*) = FilteredSampling(s, filters: _*)
    def zip(s2: Sampling) = ZipSampling(s, s2)
    def zipWithIndex(index: Prototype[Int]) = ZipWithIndexSampling(s, index)
    def take(n: Int) = TakeSampling(s, n)
    def shuffle = ShuffleSampling(s)
    def replicate[T](seeder: Factor[T, Domain[T] with Discrete[T]], replications: Int) = ReplicationSampling(s, seeder, replications)
  }

  implicit def zipWithNameFactorDecorator(factor: Factor[File, Domain[File] with Discrete[File]]) = new {
    def zipWithName(name: Prototype[String]) = ZipWithNameSampling(factor, name)
  }

  implicit def modifierFactorDecorator[T, D <: Domain[T] with Discrete[T]](f: Factor[T, D]) = new {
    def filter(filters: Filter*) = FilteredSampling(f, filters: _*)
    def zip(s2: Sampling) = ZipSampling(f, s2)
    def zipWithIndex(index: Prototype[Int]) = ZipWithIndexSampling(f, index)
    def take(n: Int) = TakeSampling(f, n)
    def shuffle = ShuffleSampling(f)
    def replicate[T2](seeder: Factor[T2, Domain[T2] with Discrete[T2]], replications: Int) = ReplicationSampling(f, seeder, replications)
  }

}