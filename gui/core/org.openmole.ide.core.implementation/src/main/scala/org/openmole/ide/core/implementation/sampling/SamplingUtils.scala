/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.sampling

import org.openmole.core.model.sampling.{ DiscreteFactor, Sampling, Factor }
import org.openmole.core.model.domain.{ Discrete, Domain }

object SamplingUtils {

  def toUnorderedFactorsAndSamplings(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]): List[Sampling] =
    toUnorderedSamplingTuples(factorOrSampling).map { _._1 }

  def toOrderedSamplings(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]): List[Sampling] =
    toUnorderedSamplingTuples(factorOrSampling).sortWith((s1, s2) ⇒ s1._2 < s2._2).map { _._1 }

  def toFactors(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]): List[Factor[_, _]] =
    factorOrSampling.map { _.left.get._1.asInstanceOf[Factor[Any, Domain[Any]]] }

  private def toUnorderedSamplingTuples(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]): List[(Sampling, Int)] =
    factorOrSampling.map {
      fos ⇒
        fos match {
          case Right(s) ⇒ s
          case Left(f) ⇒ (DiscreteFactor(f._1.asInstanceOf[Factor[Any, Domain[Any] with Discrete[Any]]]), f._2)
        }
    }

}