package org.openmole.site

import java.util.UUID

import scalatags.Text.TypedTag
import scalatags.generic.StylePair

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

  def listItem(content: Frag*): Frag = li(content)
  def htmlList(items: Frag*): Frag = ul(items)

  def paragraph(body: Frag*): Frag = Seq[Frag](body)

  def aa = a(targetBlank)

  def todo(content: String) = ""

  val break = br(br)

  object sect extends Section() {
    override val headerH1 = Seq(
      fontSize := "1.75em",
      textAlign := "left",
      // padding := "2.5em 1em 0"
      margin := "30px 0 0"
    )

    override val header = Seq(
      margin := 0,
      color := "#333",
      textAlign.center,
      padding := "2.5em 2em 0"
    )
  }

  object api {

    def apiEntryTitle(entryName: String): Frag = Seq[Frag](b(entryName), ": ")
    def newEntry(name: String, body: Frag*): Frag = Seq[Frag](apiEntryTitle(name), body)
  }

  object hl extends Highlighter {

    def apply(code: String, lang: String) = highlight(code, lang)

    object OptionalName {
      implicit def fromString(s: String) = OptionalName(Some(s))
    }

    case class OptionalName(name: Option[String])

    def openmole(code: String, header: String = "", name: OptionalName = OptionalName(None)) = {
      if (Test.testing) Test.allTests += Test(header + "\n" + code, name.name)
      highlight(code, "scala")
    }

    def code(code: String) = openmoleNoTest(code)
    def plain(code: String) = highlight(code, "plain")
    def openmoleNoTest(code: String) = highlight(code, "scala")
  }

  def openmole(code: String, header: String = "", name: hl.OptionalName = hl.OptionalName(None)) = hl.openmole(code, header, name)
  def code(code: String) = hl.code(code)
  def plain(code: String) = hl.plain(code)

  /** heavily inspired from Section.scala */
  object links {

    def anchor(elements: Seq[Any]): Seq[Modifier] =
      link(elements) match {
        case Some(t) ⇒ Seq(a(id := s"${shared.anchor(t)}", top := -90, position := "relative", display := "block"))
        case None    ⇒ Seq()
      }

    def link(elements: Seq[Any]) = elements.collect { case x: String ⇒ x }.headOption
    def linkIcon(elements: Seq[Any]): Seq[Modifier] =
      link(elements) match {
        case Some(t) ⇒ Seq(" ", a(href := s"#${shared.anchor(t)}", tag("font")(size := 4, opacity := 0.4)("\uD83D\uDD17")))
        case None    ⇒ Seq()
      }

    def toModifier(element: Any): Modifier =
      element match {
        case e: String ⇒ e
        case e: TypedTag[String] ⇒ e
        case e: scalatags.generic.StylePair[Any, String] ⇒ e.s := e.v
        case _ ⇒ throw new RuntimeException("Unknown element type " + element.getClass)
      }

  }

  object sitemap {

    def siteMapSection(docSection: Seq[Page]) = for {
      page ← docSection
    } yield li(a(page.title, href := page.file))

  }

  def h1(elements: Any*): Frag = Seq(div(links.anchor(elements): _*), scalatags.Text.all.h1(elements.map(links.toModifier): _*))
  def h2(elements: Any*): Frag = Seq(div(links.anchor(elements): _*), scalatags.Text.all.h2(`class` := shared.documentationSideMenu.cssClass)(elements.map(links.toModifier) ++ links.linkIcon(elements): _*))
  def h3(elements: Any*): Frag = Seq(div(links.anchor(elements): _*), scalatags.Text.all.h3(`class` := shared.documentationSideMenu.cssClass)(elements.map(links.toModifier) ++ links.linkIcon(elements): _*))

  def anchor(title: String) = s"#${shared.anchor(title)}"

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
  def classIs(s: String): AttrPair = `class` := s

  def to(page: Page): TypedTag[String] = to(Pages.file(page), otherTab = false)

  def to(link: String, otherTab: Boolean = true): TypedTag[String] = a(href := link)(if (otherTab) targetBlank else "")

  def innerLink(page: Page, title: String) = to(page)(span(title))

  def outerLink(linkName: String, link: String, otherTab: Boolean = true) = to(link, otherTab = true)(span(linkName))

  // CONVENIENT KEYS
  implicit class SString(ss: String) {
    def ++(s: String) = s"$ss $s"
  }

  def linkButton(title: String, link: String, buttonStyle: AttrPair = classIs(btn ++ btn_default), openInOtherTab: Boolean = true) =
    a(href := link)(if (openInOtherTab) targetBlank else "")(span(buttonStyle, `type` := "button", title))

  def divLinkButton(content: TypedTag[_], link: String, buttonStyle: AttrPair = classIs(btn ++ btn_default), openInOtherTab: Boolean = true) =
    a(href := link)(if (openInOtherTab) targetBlank else "")(span(content)(buttonStyle, `type` := "button"))

  def pageLinkButton(title: String, page: Page, openInOtherTab: Boolean = true, buttonStyle: Seq[Modifier] = Seq(classIs(btn ++ btn_default))) =
    to(page)(if (openInOtherTab) targetBlank else "")(span(buttonStyle, `type` := "button", title))

  def glyphSpan(glyphicon: String, style: Seq[Modifier], page: Page, text: String = ""): TypedTag[_ <: String] =
    to(page)(classIs(glyphicon), style, pointer, aria.hidden := "true")(text)

  def leftGlyphButton(title: String, page: Page, glyph: String, openInOtherTab: Boolean = false, buttonStyle: Seq[Modifier] = Seq(classIs(btn ++ btn_default))) =
    to(page)(if (openInOtherTab) targetBlank else "")(
      span(buttonStyle, `type` := "button")(
        span(classIs(glyph)),
        span(s" $title")
      )
    )

  def modificationLink(source: String) =
    s"https://github.com/openmole/openmole/edit/dev/openmole/bin/org.openmole.site/jvm/src/main/scalatex/$source"

  def rightGlyphButton(title: String, page: Page, glyph: String, openInOtherTab: Boolean = false, buttonStyle: Seq[Modifier] = Seq(classIs(btn ++ btn_default))) =
    to(page)(if (openInOtherTab) targetBlank else "")(
      span(buttonStyle, `type` := "button")(
        span(s"$title "),
        span(classIs(glyph))
      )
    )

  def basicButton(title: String, buttonStyle: AttrPair = classIs(btn ++ btn_default)) =
    span(buttonStyle, `type` := "button", title)

  lazy val nav: String = "nav"
  lazy val navbar: String = "navbar"
  lazy val navbar_nav: String = "navbar-nav"
  lazy val navbar_default: String = "navbar-default"
  lazy val navbar_inverse: String = "navbar-inverse"
  lazy val navbar_staticTop: String = "navbar-static-top"
  lazy val navbar_staticBottom: String = "navbar-static-bottom"
  lazy val navbar_fixedTop: String = "navbar-fixed-top"
  lazy val navbar_fixedBottom: String = "navbar-fixed-bottom"
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
  lazy val btn_mole: String = "btn-mole"

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
  lazy val row: String = "row"
  def colMD(nb: Int): String = s"col-md-$nb"

}
