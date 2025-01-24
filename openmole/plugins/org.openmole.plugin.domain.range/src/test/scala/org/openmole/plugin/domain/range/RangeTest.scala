/**
 * Created by Romain Reuillon on 29/05/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
 *
 */
package org.openmole.plugin.domain.range

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.workflow.sampling.ScalableValue
import org.scalatest.*

class RangeTest extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  import org.openmole.core.workflow.test.Stubs._

  "sizes of computed values" should "be correct" in:
    RangeDomain(0.0, 10.0, 0.1).iterator(Context()).size shouldBe 101
    RangeDomain(0, 10, 1).iterator(Context()).size shouldBe 11
    LogRangeDomain(0.0, 10.0, 10).iterator(Context()).size shouldBe 10

  "ranges using val" should "compile" in:
    val i = Val[Int]
    val rd = RangeDomain(0, i, 1)
    rd.iterator.from(Context(i -> 2)).size shouldEqual 3

    val rd2 = RangeDomain[Double](0, "i * 2", 1)
    rd2.iterator.from(Context(i -> 2)).size shouldEqual 5



