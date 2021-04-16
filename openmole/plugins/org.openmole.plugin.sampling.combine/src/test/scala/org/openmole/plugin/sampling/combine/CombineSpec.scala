package org.openmole.plugin.sampling.combine

import org.scalatest._

/*
 * Copyright (C) 2021 Romain Reuillon
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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

import org.openmole.plugin.domain.collection._

class CombineSpec extends FlatSpec with Matchers {
  import org.openmole.core.workflow.test.Stubs._

  "x keyword" should "create a complete sampling" in {
    val x1 = Val[Int]
    val x2 = Val[Double]
    val x3 = Val[String]

    val s = (x1 in (0 until 2)) x (x2 in (0.0 until 1.0 by 0.1)) x (x3 in List("a", "b"))

    s.prototypes.toSet should equal(Set(x1, x2, x3))

    s().from(Context.empty).size should equal(40)
  }

  "++ keyword" should "concatenate samplings" in {
    val x1 = Val[Int]
    val x2 = Val[Double]

    val s = (x1 in (0 until 2)) ++ (x1 in (8 until 10)) ++ ((x1 in List(100, 101)) x (x2 in List(8.9, 9.0)))

    s.prototypes.toSet should equal(Set(x1))
    s().from(Context.empty).size should equal(8)
  }

  "zip keyword" should "zip samplings" in {
    val x1 = Val[Int]
    val x2 = Val[Double]
    val x3 = Val[String]

    val s = (x1 in (0 until 2)) zip (x2 in (8.0 until 9.0 by 0.5)) zip (x3 in List("a", "b"))

    s.prototypes.toSet should equal(Set(x1, x2, x3))
    s().from(Context.empty).size should equal(2)
  }

}
