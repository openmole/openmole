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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.Task
import org.openmole.core.workflow.tools._
import org.scalatest._
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

import scala.util.Random

class VariableExpansionSpec extends FlatSpec with Matchers {

  "A expandData" should "expand all the ${} top level sequence from an inputStream and return a parsed OuputStream" in {
    val template = """My first line
${2*3}
${s"I am ${6*5} year old"}"""

    val expected = """My first line
6
I am 30 year old"""

    val res = VariableExpansion(template).expand(Context.empty)(new Random())
    res should equal(expected)
  }

  "A expandData" should "preserve additionnal $ in the string" in {
    val test = "$$$etere{etsaesrn}etasriu$$$$eatsrn$"
    val res = VariableExpansion(test).expand(Context.empty)(new Random())
    test should equal(res)
  }

}