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
package org.openmole.plugin.domain.collection

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.workflow.sampling.ScalableValue
import org.scalatest.*

class CollectionTest extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  import org.openmole.core.workflow.test.Stubs.*

  "collection" should "be  domains" in:
    val d = Val[Double]
    val scd: ScalableValue = d in (0.0 to 1.0)
    val samplingD1: Sampling = d in (0.0 to 1.0)
    samplingD1.sampling.from(Context()).size shouldEqual 2
    val samplingD2: Sampling = d in (0.0 to 10.0 by 5.0)
    samplingD2.sampling.from(Context()).size shouldEqual 3

    val i = Val[Int]
    val samplingI: Sampling = i in (0 to 1)
    samplingI.sampling.from(Context()).size shouldEqual 2



