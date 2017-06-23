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
      highlight(code, "scala")
    }

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

  def to(ref: String) = a(href := ref)

  def innerLink(page: Page, title: String) = to(page.file)(span(title))

  def buttonLink(ref: String, buttonTitle: String) = to(ref)(targetBlank)(span(classIs("btn btn-primary"), `type` := "button", buttonTitle))

  // CONVINIENT KEYS
  implicit class SString(ss: String) {
    def ++(s: String) = s"$ss $s"
  }
  
  lazy val nav: String = "nav"
  lazy val navbar: String = "navbar"
  lazy val navbar_nav: String = "navbar-nav"
  lazy val navbar_default: String = "navbar-default"
  lazy val navbar_inverse: String = "navbar-inverse"
  lazy val navbar_staticTop: String = "navbar-static-top"
  lazy val navbar_fixedTop: String = "navbar-fixed-top"
  lazy val navbar_right: String = "navbar-right"
  lazy val navbar_left: String = "navbar-left"
  lazy val navbar_header: String = "navbar-header"
  lazy val navbar_brand: String = "navbar-brand"
  lazy val navbar_btn: String = "navbar-btn"
  lazy val navbar_collapse: String = "navbar-collapse"
  lazy val container_fluid: String = "container-fluid"
  lazy val pointer = cursor := "pointer"
  lazy val targetBlank = target := "_blank"
  lazy val collapse: String = "collapse"

}
