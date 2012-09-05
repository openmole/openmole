/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.domain.modifier

import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.implementation.data._

import org.openmole.core.model.domain.IIterable
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class GroupedDomainSpec extends FlatSpec with ShouldMatchers {

  "SlicedIterablesDomain" should "change the values of a domain to iterables" in {
    val r1 = (1 to 10)

    val d1 = new IDomain[Int] with IIterable[Int] {
      override def iterator(context: Context) = r1.iterator
    }

    val md = new GroupDomain(d1, 3).iterator(Context.empty)

    md.toList.size should equal(4)
  }

}
