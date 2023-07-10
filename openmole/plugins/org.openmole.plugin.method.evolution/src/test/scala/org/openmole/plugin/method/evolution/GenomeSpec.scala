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

import org.scalatest._
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

import org.openmole.plugin.domain.collection._
import org.openmole.plugin.domain.bounds._

class GenomeSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "syntax x to y" should "be converted to bounds" in {
    val x = Val[Double]
    val g1: Genome = Seq(x in (1.0 to 2.0))
    g1.head.isInstanceOf[org.openmole.plugin.method.evolution.Genome.GenomeBound.ScalarDouble] should equal(true)

    val y = Val[Int]
    val g2: Genome = Seq(y in (1 to 20))
    g2.head.isInstanceOf[org.openmole.plugin.method.evolution.Genome.GenomeBound.ScalarInt] should equal(true)

    val g3: Genome = Seq(x in (0.5 to 1.0))
    val g3part = g3.head.asInstanceOf[org.openmole.plugin.method.evolution.Genome.GenomeBound.ScalarDouble]
    g3part.low should equal(0.5)
    g3part.high should equal(1.0)

  }

}
