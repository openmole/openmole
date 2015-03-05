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
import org.openmole.console.Console
import org.openmole.core.tools.service.ObjectPool

import scala.util.Try

object DSLTest {

  lazy val console = new Console()

  def engine = console.newREPL

  lazy val engines = new ObjectPool[ScalaREPL](engine) {
    override def release(t: ScalaREPL) = {
      t.reset
      console.initialise(t)
      super.release(t)
    }
  }

  def test(code: String, header: String) = Try {
    def testCode = s"""
        |$header
        |$code
      """.stripMargin

    engines.exec(_.compiled(testCode))
  }
}
