package org.openmole.site

/*
 * Copyright (C) 01/04/16 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package object tools {

  import scalatags.Text.all._
  import scalatex.site.{ Highlighter, Section }

  def question(content: String) = div(`class` := "question", content)

  object sect extends Section()

  object hl extends Highlighter {
    //  override def suffixMappings = Map().withDefault(identity)

    def openmole(code: String, test: Boolean = true, header: String = "") = {
      if (Test.testing && test) Test.allTests += code
      highlight(code, "scala")
    }

    def code(code: String) = openmoleNoTest(code)
    def openmoleNoTest(code: String) = openmole(code, test = false)
  }

  case class Parameter(name: String, `type`: String, description: String)

  def parameters(p: Parameter*) = {
    def toRow(p: Parameter) = li(p.name + ": " + p.`type` + ": " + p.description)

    ul(p.map(toRow))
  }

  def tq = """""""""

  // SCALATAGS METHODS

  def classIs(s: String) = `class` := s

  def styleIs(s: String) = `style` := s

  val targetBlank = target := "_blank"

  def to(ref: String) = a(href := ref)

  def innerLink(page: Page, title: String) = to(page.file)(span(title))

  def buttonLink(ref: String, buttonTitle: String) = to(ref)(targetBlank)(span(classIs("btn btn-primary"), `type` := "button", buttonTitle))

}
