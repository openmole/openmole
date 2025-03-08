package org.openmole.plugin.method.evolution

/*
 * Copyright (C) 2021 Romain Reuillon
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
import org.openmole.plugin.domain.collection.{*, given}
import org.openmole.plugin.domain.bounds.{*, given}
import org.openmole.plugin.method.evolution.HDOSE.OriginAxe.genomeBound

import org.scalatest.*

class GenomeSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "Genome" should "support the following cases" in:
    def genomeBound(g: Genome.GenomeBound) = g

    val x = Val[Double]
    genomeBound(x in (1.0 to 2.0)) shouldBe a [org.openmole.plugin.method.evolution.Genome.GenomeBound.ScalarDouble]
    genomeBound(x in (0.0, 99.0)) shouldBe a [org.openmole.plugin.method.evolution.Genome.GenomeBound.ScalarDouble]
    genomeBound(x in (0.5 to 1.0)) should matchPattern:
      case g: Genome.GenomeBound.ScalarDouble if g.low == 0.5 && g.high == 1.0 =>

    val y = Val[Int]
    genomeBound(y in (1 to 20)) shouldBe a [org.openmole.plugin.method.evolution.Genome.GenomeBound.ScalarInt]

    val vx = Val[Array[Double]]
    genomeBound(vx in Seq.fill(10)(0.0 to 1.0)) shouldBe a [org.openmole.plugin.method.evolution.Genome.GenomeBound.SequenceOfDouble]
    genomeBound(vx in Seq.fill(10)(0.0 to 1.0)) should matchPattern:
      case s: org.openmole.plugin.method.evolution.Genome.GenomeBound.SequenceOfDouble if s.size == 10 =>

    genomeBound(vx in Seq.fill(100)(0.0, 99.0)) shouldBe a[org.openmole.plugin.method.evolution.Genome.GenomeBound.SequenceOfDouble]

    val ai = Val[Array[Int]]
    genomeBound(ai in Seq.fill(10)(0 to 100)) shouldBe a [org.openmole.plugin.method.evolution.Genome.GenomeBound.SequenceOfInt]
    genomeBound(ai in Seq.fill(10)(0 to 100)) should matchPattern:
      case s: org.openmole.plugin.method.evolution.Genome.GenomeBound.SequenceOfInt if s.size == 10 =>


  "OSE Origin Axe" should "support the following cases" in :

    def originAxe(o: OSE.OriginAxe) = o

    val ad = Val[Double]

    originAxe(ad in (0.0 to 10.0)) shouldBe a[OSE.ScalarDoubleOriginAxe]

    val ar = Val[Array[Double]]
    originAxe(ar in Vector.fill(10)(0.0 to 10.0)) shouldBe a[OSE.SequenceOfDoubleOriginAxe]

    val i1 = Val[Int]
    originAxe(i1 in (0 to 10)) shouldBe a [OSE.ScalarIntOriginAxe]
    originAxe(i1 in (0.0 to 10.0)) shouldBe a [OSE.ContinuousIntOriginAxe]

    val ai2 = Val[Array[Int]]
    originAxe(ai2 in Vector.fill(10)(0 to 100)) shouldBe a[OSE.SequenceOfIntOriginAxe]
    originAxe(ai2 in Vector.fill(10)(0.0 to 100.0)) shouldBe a[OSE.SequenceOfContinuousIntOriginAxe]

    val bo = Val[Boolean]
    originAxe(bo in TrueFalse) shouldBe a[OSE.EnumerationOriginAxe]


  "HDOSE Origin Axe" should "support the following cases" in:
    def originAxe(o: HDOSE.OriginAxe) = o

    val ad = Val[Double]

    originAxe(ad in (0.0 to 10.0)) shouldBe a [HDOSE.ScalarDoubleOriginAxe]
    originAxe(ad in (0.0 to 10.0 weight 5.0)) shouldBe a [HDOSE.ScalarDoubleOriginAxe]

    val ar = Val[Array[Double]]
    originAxe(ar in Vector.fill(10)(0.0 to 10.0)) shouldBe a [HDOSE.SequenceOfDoubleOriginAxe]
    originAxe(ar in Vector.fill(10)(0.0 to 10.0 weight 2.0)) shouldBe a [HDOSE.SequenceOfDoubleOriginAxe]

    val i1 = Val[Int]
    originAxe(i1 in (0 to 10)) shouldBe a [HDOSE.ScalarIntOriginAxe]
    originAxe(i1 in (0 to 10 weight 5.0)) shouldBe a [HDOSE.ScalarIntOriginAxe]
    originAxe(i1 in (0.0 to 10.0)) shouldBe a [HDOSE.ContinuousIntOriginAxe]
    originAxe(i1 in (0.0 to 10.0 weight 5.0)) shouldBe a [HDOSE.ContinuousIntOriginAxe]

    val ai2 = Val[Array[Int]]
    originAxe(ai2 in Vector.fill(10)(0 to 100)) shouldBe a [HDOSE.SequenceOfIntOriginAxe]
    originAxe(ai2 in Vector.fill(10)(0 to 100 weight 20.0))  shouldBe a [HDOSE.SequenceOfIntOriginAxe]

    val bo = Val[Boolean]
    originAxe(bo in TrueFalse) shouldBe a [HDOSE.EnumerationOriginAxe]
    originAxe(bo in (TrueFalse weight 10.0)) shouldBe a [HDOSE.EnumerationOriginAxe]

    val bao = Val[Array[Boolean]]
    originAxe(bao in Vector.fill(10)(TrueFalse)) shouldBe a [HDOSE.SequenceOfEnumerationOriginAxe]
    originAxe(bao in Vector.fill(10)(TrueFalse weight 10.0)) shouldBe a [HDOSE.SequenceOfEnumerationOriginAxe]
    originAxe(bao in Vector.fill(10)(TrueFalse weight 10.0)) should matchPattern:
      case s: HDOSE.SequenceOfEnumerationOriginAxe if s.p.values.size == 10 =>





  "OSE fitness pattern" should "support the following cases" in:
    def pattern(p: OSE.FitnessPattern) = p

    val o = Val[Double]
    pattern(o evaluate "o.map(x => math.abs(x - 28.0)).max" under 1)
