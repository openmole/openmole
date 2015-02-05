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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package documentation

import javax.script.ScriptEngineManager

import org.openmole.misc.tools.service.ObjectPool
import scala.tools.nsc.interpreter.IMain
import util.Try

object DSLTest {

  def engine = {
    val eng = new ScriptEngineManager().getEngineByName("scala").asInstanceOf[IMain]
    val settings = eng.settings
    class Plop
    settings.embeddedDefaults[Plop]
    eng
  }

  lazy val engines = new ObjectPool[IMain](engine) {
    override def release(t: IMain) = {
      t.reset
      super.release(t)
    }
  }

  def test(code: String, header: String) = Try {
    def testCode = s"""import org.openmole.core.dsl._
        |
        |$header
        |$code
      """.stripMargin

    engines.exec(_.compiled(testCode))
  }
}
