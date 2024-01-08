package org.openmole.core.omr

import java.util.concurrent.atomic.AtomicInteger

import org.openmole.core.context.*
import org.scalatest.*


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

class OMRSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  "OMR" should "intercept the execution of a task" in:
    val p = Val[String]

    //executed.get should equal(1)
  

