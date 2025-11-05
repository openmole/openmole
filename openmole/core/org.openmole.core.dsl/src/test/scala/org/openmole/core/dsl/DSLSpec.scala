package org.openmole.core.dsl

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

import org.scalatest._
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

class DSLSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "DSL" should "convert values as expected" in:
    val x = Val[Long]
    val testFromContext: FromContext[Long] = x
    val testOptionalArgument: OptionalArgument[FromContext[Long]] = x
    val testOptionalFromContextLong: OptionalArgument[FromContext[Long]] = 42
    val testOptionalFromContextBoolean: OptionalArgument[Condition] = true
    val testoptionlaFromContextString: OptionalArgument[FromContext[String]] = "test"
  

  "range of double" should "be of correct size" in {
    val r = (0.0 to 10.0 by 0.2)
    assert(r.size == 51)
  }

}
