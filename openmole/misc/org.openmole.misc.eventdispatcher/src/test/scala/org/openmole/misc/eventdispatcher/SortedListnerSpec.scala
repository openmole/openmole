/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
 */

package org.openmole.misc.eventdispatcher

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class SortedListnerSpec extends FlatSpec with ShouldMatchers {
  "Sorted listner" should "sort listner by priority" in {
    val sortedListners = new SortedListeners[String]
    sortedListners.register(4, "Test4")
    sortedListners.register(2, "Test2")
    sortedListners.register(10, "Test10")

    (sortedListners.head == "Test10") should equal(true)
    (sortedListners.last == "Test2") should equal(true)

  }

  "A listner" should "be removed" in {
    val sortedListners = new SortedListeners[String]
    sortedListners.register(4, "Test4")
    sortedListners.register(2, "Test2")
    sortedListners.register(10, "Test10")

    sortedListners -= "Test10"

    (sortedListners.size) should equal(2)

  }

}
