/*
 * Copyright (C) 2014 Romain Reuillon
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

import org.openmole.misc.exception.UserBadDataError

import scala.util.Failure
import scalatex.site._
import scalatags.Text.all._

object Objects {
  object sect extends Section()
  object hl extends Highlighter {
    def suffixMappings = Map().withDefault(identity)
    def openmole(code: String, test: Boolean = true, header: String = "") = {
      if (test)
        DSLTest.test(code, header) match {
          case Failure(f) ⇒ throw new UserBadDataError(f, s"Error testing code:\n$code")
          case _          ⇒
        }
      highlight(code, "scala")
    }
  }
  case class Parameter(name: String, `type`: String, description: String)
  def parameters(p: Parameter*) = {
    def toRow(p: Parameter) = li(p.name + ": " + p.`type` + ": " + p.description)
    ul(p.map(toRow))
  }
}
