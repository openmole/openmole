/**
 * Created by Romain Reuillon on 12/01/16.
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
package org.openmole.plugin.domain.modifier

import org.openmole.core.context.Context
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class TakeDomainSpec extends FlatSpec with Matchers {

  "MapDomain" should "take the 10 first value of the domain" in {
    implicit val rng = new Random(42)

    val r1 = (1 to 30)
    val md = TakeDomain(r1, 10).computeValues().from(Context.empty)(RandomProvider(???))

    md.toList == r1.size should equal(10)
  }

}
