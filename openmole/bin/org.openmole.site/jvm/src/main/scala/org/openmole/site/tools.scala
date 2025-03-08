package org.openmole.site

import org.openmole.core.exception.InternalProcessingError

import java.util.UUID
import scalatags.Text.TypedTag
import scalatags.generic.StylePair

import collection.mutable.ListBuffer

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

object tools {

  import scalatags.Text.{all => tags}
  import scalatags.Text.all._

  def listItem(content: Frag*): Frag = li(content)
  def htmlList(items: Frag*): Frag = ul(items)

  def paragraph(body: Frag*): Frag = Seq[Frag](body)

  def aa = a(targetBlank)

  def mandatory = b{"mandatory"}
  def optional = b{"optional"}

  def indice(v: Frag, i: Frag) = html"$v${sub{i}}"

  def comment(c: String): Frag = ""

  object hl:

    def apply(content: String, lang: String, clazz: Option[String] = Some("doc-code")) = highlight(content, lang)

    def highlight(string: String, lang: String, clazz: Option[String] = Some("doc-code")) =
      val lines = string.split("\n", -1)

      val modif: Seq[Modifier] = clazz.toSeq.map(c => cls := c)

      if lines.length == 1
      then
        scalatags.Text.all.code(
          cls := lang + " " + "hljs",
          modif,
          display := "inline",
          padding := 0,
          margin := 0,
          lines(0))
      else
        val minIndent = lines.filter(_.trim != "").map(_.takeWhile(_ == ' ').length).min
        val stripped = lines.map(_.drop(minIndent))
          .dropWhile(_ == "")
          .mkString("\n")

        pre(
          modif,
          scalatags.Text.all.code(
            cls := lang + " " + "hljs",
            stripped)
        )

    object OptionalName:
      implicit def fromString(s: String): OptionalName = OptionalName(Some(s))

    case class OptionalName(name: Option[String])

    def openmole(code: String, header: String = "", name: OptionalName = OptionalName(None))(using sourceFile: sourcecode.File, sourceLine: sourcecode.Line) =
      Test.list(Test(header + "\n" + code, name.name, sourceFile.value, sourceLine.value))
      apply(code, "scala")

    def code(code: String) = openmoleNoTest(code)

    def plain(code: String) = apply(code, "plain")
    def openmoleNoTest(code: String) = apply(code, "scala")
    def python(code: String) = apply(code, "python")
    def json(code: String) = apply(code, "json")


  def openmole(code: String, header: String = "", name: hl.OptionalName = hl.OptionalName(None)) = hl.openmole(code, header, name)
  def code(code: String) = hl.code(code)
  def plain(code: String) = hl.plain(code)

  /** heavily inspired from Section.scala **/
  object links:

    def anchor(elements: Seq[Any]): Seq[Modifier] =
      link(elements) match
        case Some(t) => Seq(a(id := s"${shared.anchor(t)}", top := -60, position := "relative", display := "block"))
        case None    => Seq()

    def link(elements: Seq[Any]) = elements.collect { case x: String => x }.headOption
    def linkIcon(elements: Seq[Any]): Seq[Modifier] =
      link(elements) match
        case Some(t) => Seq(" ", a(href := s"#${shared.anchor(t)}", tag("font")(size := 4, opacity := 0.4)("\uD83D\uDD17")))
        case None    => Seq()

    def toModifier(element: Any): Modifier =
      element match
        case e: String => e
        case e: TypedTag[String] => e
        case e: scalatags.generic.StylePair[Any, String] => e.s := e.v
        case e: AttrPair => e
        case e: SeqFrag[_] => e
        case _ => throw new RuntimeException("Unknown element type " + element.getClass)

  object sitemap {

    def siteMapSection(docSection: Seq[Page]) = for {
      page ← docSection
    } yield li(a(page.title, href := page.file))

  }

  def h2(elements: Any*): Frag = Seq(div(links.anchor(elements) *), scalatags.Text.all.h2(elements.map(links.toModifier) ++ links.linkIcon(elements) *))
  def h3(elements: Any*): Frag = Seq(div(links.anchor(elements) *), scalatags.Text.all.h3(elements.map(links.toModifier) ++ links.linkIcon(elements) *))

  def anchor(title: String) = s"#${shared.anchor(title)}"

  def img(xs: Modifier*) = scalatags.Text.all.img(Seq(cls := "doc-img") ++ xs *)
  def br = scalatags.Text.all.br(cls := "doc-br")

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
  def classIs(s: String*): AttrPair = `class` := s.mkString(" ")

  def to(page: Page): TypedTag[String] = to(Pages.file(page), otherTab = false)

  def to(link: String, otherTab: Boolean = true, style: Seq[Modifier] = Seq()): TypedTag[String] = a(style, href := link)(if (otherTab) targetBlank else "")

  def innerLink(page: Page, title: String) = to(page)(span(title))

  def outerLink(linkName: String, link: String, otherTab: Boolean = true) = to(link, otherTab = otherTab)(span(linkName))

  // CONVENIENT KEYS
  implicit class SString(ss: String) {
    def ++(s: String) = s"$ss $s"
  }

  def linkButton(title: String, link: String, buttonStyle: AttrPair = classIs(btn, btn_selected), openInOtherTab: Boolean = true) =
    a(href := link)(if (openInOtherTab) targetBlank else "")(span(buttonStyle, `type` := "button", title))

  def divLinkButton(content: TypedTag[_], link: String, buttonStyle: AttrPair = classIs(btn, btn_default), openInOtherTab: Boolean = true) =
    a(href := link)(if (openInOtherTab) targetBlank else "")(span(content)(buttonStyle, `type` := "button"))

  def pageLinkButton(title: String, page: Page, openInOtherTab: Boolean = true, buttonStyle: Seq[Modifier] = Seq(classIs(btn, btn_default))) =
    to(page)(if (openInOtherTab) targetBlank else "")(span(buttonStyle, `type` := "button", title))

  def glyphSpan(glyphicon: String, style: Seq[Modifier], page: Page, text: String = ""): TypedTag[_ <: String] =
    to(page)(classIs(glyphicon), style, pointer, aria.hidden := "true")(text)

  def leftGlyphButton(title: String, page: Page, glyph: String, openInOtherTab: Boolean = false, buttonStyle: Seq[Modifier] = Seq(classIs(btn, btn_default))) =
    to(page)(if (openInOtherTab) targetBlank else "")(
      span(buttonStyle, `type` := "button")(
        span(classIs(glyph)),
        span(s" $title")
      )
    )

  
  def sourceLink(source: String) = 
    if(org.openmole.core.buildinfo.version.isDevelopment) s"https://github.com/openmole/openmole/tree/dev/$source"
    else s"https://github.com/openmole/openmole/tree/${org.openmole.core.buildinfo.version.major}-dev/$source"


  def modificationLink(source: String) =
    if(org.openmole.core.buildinfo.version.isDevelopment) s"https://github.com/openmole/openmole/edit/dev/openmole/bin/org.openmole.site/jvm/src/main/scala/$source"
    else s"https://github.com/openmole/openmole/edit/${org.openmole.core.buildinfo.version.major}-dev/openmole/bin/org.openmole.site/jvm/src/main/scala/$source"

  def rightGlyphButton(title: String, page: Page, glyph: String, openInOtherTab: Boolean = false, buttonStyle: Seq[Modifier] = Seq(classIs(btn, btn_default))) =
    to(page)(if (openInOtherTab) targetBlank else "")(
      span(buttonStyle, `type` := "button")(
        span(s"$title "),
        span(classIs(glyph))
      )
    )

  def basicButton(title: String, buttonStyle: AttrPair = classIs(btn, btn_default)) =
    span(buttonStyle, `type` := "button", title)

  /*def getPageTitle(page: Page) = page.title match {
    case None    => page.name
    case Some(x) => x
  }*/

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
  lazy val btn_selected: String = "btn-selected"
  lazy val btn_primary: String = "btn-primary"
  lazy val btn_danger: String = "btn-danger"
  lazy val btn_mole: String = "btn-mole"

  lazy val glyph_chevron_left: String = "glyphicon glyphicon-chevron-left"
  lazy val glyph_chevron_right: String = "glyphicon glyphicon-chevron-right"

  lazy val role_tablist = tags.role :="tablist"
  lazy val role_presentation = tags.role :="presentation"
  lazy val role_tab = tags.role :="tab"
  lazy val tab_pane: String = "tab-pane"
  lazy val tab_panel_role = tags.role :="tabpanel"
  lazy val role_button = tags.role := "button"

  lazy val container_fluid: String = "container-fluid"
  lazy val pointer = cursor := "pointer"
  lazy val fixedPosition = position := "fixed"
  lazy val targetBlank = target := "_blank"
  lazy val collapse: String = "collapse"
  lazy val fade: String = "fade"
  lazy val row: String = "row"
  def colMD(nb: Int): String = s"col-md-$nb"


  implicit class HtmlHelper(val sc: StringContext) extends AnyVal:
    def html(args: Any*): Frag =
      def anyToFrag(a: Any): Frag =
        a match
          case s: String => (s.stripMargin: Frag)
          case f: Frag => f
          case a => throw new InternalProcessingError(s"Cannot convert $a of type ${a.getClass} into html frag")

      val strings = sc.parts.iterator
      val expressions = args.iterator
      val buf = ListBuffer[Frag](anyToFrag(strings.next()))
      while strings.hasNext
      do
        buf.append(anyToFrag(expressions.next()))
        buf.append(anyToFrag(strings.next()))

      buf.toSeq

}
