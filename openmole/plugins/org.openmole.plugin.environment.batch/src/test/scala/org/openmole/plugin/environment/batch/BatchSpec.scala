/*
 * Copyright (C) 16/02/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.batch

import org.openmole.core.dsl.*
import org.openmole.plugin.environment.batch.environment.AccessControl
import org.scalatest.*

import gears.async.*
import gears.async.default.given

import scala.util.Try

class BatchSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:

  import org.openmole.core.workflow.test.Stubs.*

  "Access control" should "detect same thread" in:
    val control = AccessControl(10)
    AccessControl.defaultPrirority:
      control:
        control:
          control.semaphore.permits should equal(9)

    control.semaphore.permits should equal(10)

    AccessControl.defaultPrirority:
      Async.blocking:
        Future:
          control:
            control:
              control.semaphore.permits should equal(9)
        .await


    control.semaphore.permits should equal(10)

    AccessControl.defaultPrirority:
      Async.blocking:
        Future:
          control:
            Thread.sleep(10)
            control:
              control.semaphore.permits should equal(9)
        .await

    control.semaphore.permits should equal(10)

    AccessControl.defaultPrirority:
      Async.blocking:
          control:
            Future:
              control:
                control.semaphore.permits should equal(9)
            .await


    control.semaphore.permits should equal(10)