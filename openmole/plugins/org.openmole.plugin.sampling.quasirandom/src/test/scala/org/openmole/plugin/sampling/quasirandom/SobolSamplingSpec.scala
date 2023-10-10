package org.openmole.plugin.sampling.quasirandom

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.scalatest.*

/*
 * Copyright (C) 2023 Romain Reuillon
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

class SobolSequenceSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:
  import org.openmole.core.workflow.test.Stubs.*

  "sobol sequence" should "start with the center point" in:
    SobolSampling.sobolValues(4, 1).next() should equal(Seq.fill(4)(0.5))
    SobolSampling.sobolValues(4, 10).toSeq.size should equal(10)















