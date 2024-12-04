package org.openmole.plugin.sampling.combine

/*
 * Copyright (C) 2024 Romain Reuillon
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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

object TupledSampling:
  given tuple2InIsSampling[T1, T2, D](using d: DiscreteFromContextDomain[D, (T1, T2)]): IsSampling[In[(Val[T1], Val[T2]), D]] = s =>
    import org.openmole.plugin.domain.collection._
    import org.openmole.plugin.domain.modifier._
    import cats.implicits.*
    val d1v = d(s.domain).domain.map(_.map(_._1))
    val d2v = d(s.domain).domain.map(_.map(_._2))

    val (v1, v2) = s.value
    
    ZipSampling(v1 in d1v, v2 in d2v)


  given tuple3InIsSampling[T1, T2, T3, D](using d: DiscreteFromContextDomain[D, (T1, T2, T3)]): IsSampling[In[(Val[T1], Val[T2], Val[T3]), D]] = s =>
    import org.openmole.plugin.domain.collection._
    import org.openmole.plugin.domain.modifier._
    import cats.implicits.*
  
    val d1v = d(s.domain).domain.map(_.map(_._1))
    val d2v = d(s.domain).domain.map(_.map(_._2))
    val d3v = d(s.domain).domain.map(_.map(_._3))
  
    val (v1, v2, v3) = s.value
  
    (v1 in d1v) zip (v2 in d2v) zip (v3 in d3v)

  given tuple4InIsSampling[T1, T2, T3, T4, D](using d: DiscreteFromContextDomain[D, (T1, T2, T3, T4)]): IsSampling[In[(Val[T1], Val[T2], Val[T3], Val[T4]), D]] = s =>
    import org.openmole.plugin.domain.collection._
    import org.openmole.plugin.domain.modifier._
    import cats.implicits.*
    
    val d1v = d(s.domain).domain.map(_.map(_._1))
    val d2v = d(s.domain).domain.map(_.map(_._2))
    val d3v = d(s.domain).domain.map(_.map(_._3))
    val d4v = d(s.domain).domain.map(_.map(_._4))

    val (v1, v2, v3, v4) = s.value

    (v1 in d1v) zip (v2 in d2v) zip (v3 in d3v) zip (v4 in d4v)