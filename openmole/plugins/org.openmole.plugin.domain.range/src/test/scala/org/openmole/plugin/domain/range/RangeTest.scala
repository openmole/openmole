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

import org.openmole.core.context.Context
import org.scalatest._

class RangeTest extends FlatSpec with Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "sizes of computed values" should "be correct" in {
    Range[Double](0.0, 10.0, 0.1).computeValues(Context()).size shouldBe 101
    Range[Int](0, 10, 1).computeValues(Context()).size shouldBe 11
    LogRange[Double](0.0, 10.0, 10).computeValues(Context()).size shouldBe 10
  }

}
