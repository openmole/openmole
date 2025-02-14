package org.openmole.plugin.sampling.onefactor

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.scalatest._
import org.openmole.plugin.domain.collection.{*, given}

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

class OneFactorSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {
  import org.openmole.core.workflow.test.Stubs._

  "x keyword" should "create a complete sampling" in {
    val x1 = Val[Double]
    val x2 = Val[Double]
    val x3 = Val[Double]

    val s =
      OneFactorSampling(
        (x1 in (0.0 until 1.0 by 0.1)) nominal 0.5,
        (x2 in (0.0 until 1.0 by 0.1)) nominal 0.5,
        (x3 in (0.0 until 1.0 by 0.1)) nominal 0.5
      )

    (s: Sampling).outputs.toSet should equal(Set(x1, x2, x3))

    (s: Sampling).sampling.from(Context.empty).size should equal(30)
  }
}
