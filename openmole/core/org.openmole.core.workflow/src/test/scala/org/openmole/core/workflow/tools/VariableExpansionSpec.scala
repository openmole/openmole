/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.core.workflow.tools

import org.openmole.core.context.*
import org.openmole.core.fromcontext.{ExpandedString, FromContext}
import org.scalatest.*
import org.openmole.core.workflow.dsl.*
import org.openmole.tool.cache.Lazy

import scala.util.Random

class VariableExpansionSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "A expandData" should "expand all the ${} top level sequence from an inputStream and return a parsed OuputStream" in {
    val template = """My first line
${2*3}
${s"I am ${6*5} years old"}"""

    val expected = """My first line
6
I am 30 years old"""

    val res = ExpandedString(template).from(Context.empty)
    res should equal(expected)
  }

  "A expandData" should "preserve additionnal $ in the string" in {
    val test = "$$$etere{etsaesrn}etasriu$$$$eatsrn$"
    val res = ExpandedString(test).from(Context.empty)
    test should equal(res)
  }

  "Expansion" should "preserve accents" in {
    val test = "tést"
    val res = ExpandedString(test).from(Context.empty)
    test should equal(res)
  }

  "String" should "be expanded" in {
    val e = ExpandedString("${x} times 2 equals ${x * 2}")
    val x = Val[Int]
    e(Context(x -> 2)) should equal("2 times 2 equals 4")

    val e2: FromContext[String] = "${x} times 2 equals ${x * 2}"
    e2(Context(x -> 2)) should equal("2 times 2 equals 4")
  }

}