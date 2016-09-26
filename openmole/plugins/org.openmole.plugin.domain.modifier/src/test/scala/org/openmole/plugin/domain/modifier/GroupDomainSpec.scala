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

import org.openmole.core.context.Context
import org.openmole.core.workflow.domain._
import org.openmole.tool.random.RandomProvider
import org.scalatest._

import scala.util.Random

class GroupDomainSpec extends FlatSpec with Matchers {

  "GroupDomain" should "change the values of a domain to an iterable of array" in {
    implicit val rng = new Random(42)

    val r1 = (1 to 10)

    val md = GroupDomain(r1, 3)

    def it[D](d: D)(implicit domain: Discrete[D, Array[Int]]) = domain.iterator(d).from(Context.empty)(RandomProvider(???))

    it(md).toList.size should equal(4)
  }

}
