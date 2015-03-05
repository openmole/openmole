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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.data._

import org.scalatest._

import scala.util.Random

class MapDomainSpec extends FlatSpec with Matchers {

  "MapDomain" should "change the values of a domain using groovy code" in {
    implicit val rng = new Random(42)

    val r1 = (1 to 3)

    val d1 = new Domain[Int] with Discrete[Int] {
      override def iterator(context: Context)(implicit rng: Random) = r1.iterator
    }

    val md = MapDomain(d1, "p1", "p1 * 2").iterator(Context.empty)

    md.toList == r1.map { _ * 2 } should equal(true)
  }

}
