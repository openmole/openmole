package org.openmole.site

import java.util.UUID

import scalatags.Text.TypedTag

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

    object OptionalName {
      implicit def fromString(s: String) = OptionalName(Some(s))
    }

    case class OptionalName(name: Option[String])

    def openmole(code: String, header: String = "", name: OptionalName = OptionalName(None)) = {
      if (Test.testing) Test.allTests += Test(header + "\n" + code, name.name)
      highlight(code, "scala")
    }

    def code(code: String) = openmoleNoTest(code)
    def openmoleNoTest(code: String) = highlight(code, "scala")
  }

  case class Parameter(name: String, `type`: String, description: String)

  def parameters(p: Parameter*) = {
    def toRow(p: Parameter) = li(p.name + ": " + p.`type` + ": " + p.description)

    ul(p.map(toRow))
  }

  def tq = """""""""

  def uuID: String = UUID.randomUUID.toString

  implicit class ShortID(id: String) {
    def short = id.split('-').head
  }

  // SCALATAGS METHODS
  def classIs(s: String) = `class` := s

  def to(page: Page) = a(href := page.file)

  def innerLink(page: Page, title: String) = to(page)(span(title))

  // CONVINIENT KEYS
  implicit class SString(ss: String) {
    def ++(s: String) = s"$ss $s"
  }

  def siteButton(buttonStyle: AttrPair) = scalatags.Text.all.button(buttonStyle, `type` := "button")

  def linkButton(title: String, link: String, buttonStyle: AttrPair = classIs(btn ++ btn_default), openInOtherTab: Boolean = true) =
    a(href := link)(targetBlank)(span(classIs("btn btn-primary"), `type` := "button", title))

  def glyphSpan(glyphicon: String, style: Seq[Modifier], page: Page, text: String = ""): TypedTag[_ <: String] =
    to(page)(classIs(glyphicon), style, pointer, aria.hidden := "true")(text)

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
  lazy val nav_pills: String = "nav-pills"

  lazy val btn: String = "btn"
  lazy val btn_default: String = "btn-default"
  lazy val btn_primary: String = "btn-primary"
  lazy val btn_danger: String = "btn-danger"

  lazy val glyph_chevron_left: String = "glyphicon glyphicon-chevron-left"
  lazy val glyph_chevron_right: String = "glyphicon glyphicon-chevron-right"

  private def role(suffix: String): AttrPair = scalatags.Text.all.role := suffix

  lazy val role_tablist = role("tablist")
  lazy val role_presentation = role("presentation")
  lazy val role_tab = role("tab")
  lazy val tab_pane: String = "tab-pane"
  lazy val tab_panel_role = role("tabpanel")
  lazy val role_button = role("button")

  lazy val container_fluid: String = "container-fluid"
  lazy val pointer = cursor := "pointer"
  lazy val fixedPosition = position := "fixed"
  lazy val targetBlank = target := "_blank"
  lazy val collapse: String = "collapse"
  lazy val fade: String = "fade"

}
