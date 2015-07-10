/*
 * Copyright (C) 2015 Romain Reuillon
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

package org.openmole.site

import javax.script.ScriptEngineManager

import org.openmole.core.console.ScalaREPL
import org.openmole.console.{ ConsoleVariables, Console }
import org.openmole.core.tools.service.ObjectPool

import scala.collection.mutable.ListBuffer
import scala.util.Try

object DSLTest {

  case class Test(code: String, header: String, number: Int) {
    def toCode =
      s"""def test${number}: Unit = {
$header
$code
}"""
  }

  val toTest = ListBuffer[Test]()

  lazy val console = new Console()

  def engine = console.newREPL(ConsoleVariables.empty)

  def test(code: String, header: String) = toTest.synchronized {
    toTest += Test(code, header, toTest.size)
  }

  def testCode = toTest.map { _.toCode }.mkString("\n")

  def runTest = Try {
    engine.compiled(testCode)
  }

  def clear = toTest.clear

}
