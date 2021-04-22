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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.domain.modifier._
import org.scalatest._

class RangeTest extends FlatSpec with Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "sizes of computed values" should "be correct" in {
    RangeDomain[Double](0.0, 10.0, 0.1).iterator(Context()).size shouldBe 101
    RangeDomain[Int](0, 10, 1).iterator(Context()).size shouldBe 11
    LogRangeDomain[Double](0.0, 10.0, 10).iterator(Context()).size shouldBe 10
  }

  "ranges using to in scala" should "be range domains" in {
    val r1: RangeDomain[Double] = 0.0 to 1.0
    val r2: RangeDomain[Int] = 0 to 10
    val r3: RangeDomain[Double] = 0 to 10

    val i = Val[Double]
    val scalar: ScalarOrSequenceOfDouble = i in (0.0 to 1.0)
  }

  "range" should "work with modifiers" in {
    RangeDomain[Double](0.0, 10.0, 0.1).map(x ⇒ x * x)
    RangeDomain[Int](0, 10).map(x ⇒ x * x)

  }

}
