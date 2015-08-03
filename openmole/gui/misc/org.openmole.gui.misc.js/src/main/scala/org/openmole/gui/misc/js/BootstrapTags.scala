package org.openmole.gui.misc.js

/*
 * Copyright (C) 03/11/14 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.scalajs.dom.html.Div
import org.scalajs.dom.raw
import org.scalajs.dom.raw._
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.TypedTag

import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._

import org.openmole.gui.misc.js.JsRxTags._
import org.scalajs.jquery.jQuery
import org.scalajs.jquery.JQuery
import fr.iscpif.scaladget.mapping.BootstrapStatic
import rx._

@JSExport("BootstrapTags")
object BootstrapTags {

  implicit def jq2BootstrapStatic(jq: JQuery): BootstrapStatic = jq.asInstanceOf[BootstrapStatic]

  implicit def stringToClassKeyAggregator(s: String): ClassKeyAggregator = key(s)

  implicit def formTagToNode(tt: HtmlTag): org.scalajs.dom.Node = tt.render

  implicit class BootstrapTypedTag[+Output <: raw.Element](t: TypedTag[Output]) {
    def +++(m: Seq[Modifier]) = t.copy(modifiers = t.modifiers :+ m.toSeq)
  }

  def uuID: String = java.util.UUID.randomUUID.toString

  def emptyCK = ClassKeyAggregator.empty

  def key(s: String) = new ClassKeyAggregator(s)

  //Div
  def div(keys: ClassKeyAggregator = emptyCK) = tags.div(`class` := keys.key)

  //span
  def span(keys: ClassKeyAggregator = emptyCK) = tags.span(`class` := keys.key)

  // Nav
  class NavItem(val navid: String, contentDiv: TypedTag[HTMLElement], ontrigger: () ⇒ Unit, val todo: () ⇒ Unit = () ⇒ {}, extraRenderPair: Seq[Modifier] = Seq(), active: Boolean = false) {
    val activeString = {
      if (active) "active" else ""
    }

    val render = li(role := "presentation", id := navid, `class` := activeString)(tags.a(href := "", onclick := { () ⇒
      ontrigger()
      false
    }
    )(contentDiv).render)(
      extraRenderPair: _*)

  }

  def dialogNavItem(id: String, content: String, ontrigger: () ⇒ Unit = () ⇒ {}, todo: () ⇒ Unit = () ⇒ {}) =
    navItem(id, content, ontrigger, todo, Seq(data("toggle") := "modal", data("target") := "#" + id + "PanelID"))

  def navItem(id: String, content: String, ontrigger: () ⇒ Unit = () ⇒ {}, todo: () ⇒ Unit = () ⇒ {}, extraRenderPair: Seq[Modifier] = Seq(), active: Boolean = false) =
    new NavItem(id, tags.div(content), ontrigger, todo, extraRenderPair, active)

  def dialogGlyphNavItem(id: String, glyphIcon: ClassKeyAggregator, ontrigger: () ⇒ Unit = () ⇒ {}, todo: () ⇒ Unit = () ⇒ {}, extraRenderPair: Seq[Modifier] = Seq(), active: Boolean = false) =
    new NavItem(id, glyph(glyphIcon), ontrigger, todo, Seq(data("toggle") := "modal", data("target") := "#" + id + "PanelID"))

  def nav(uuid: String, keys: ClassKeyAggregator, contents: NavItem*): TypedTag[HTMLElement] =
    ul(`class` := "nav " + keys.key, id := uuid, role := "tablist")(
      contents.map { c ⇒
        c.render(scalatags.JsDom.attrs.onclick := { () ⇒
          jQuery("#" + uuid + " .active").removeClass("active")
          jQuery("#mainNavItemID").addClass("active")
          c.todo()
        })
      }: _*)

  val nav_default = key("navbar-default")
  val nav_inverse = key("navbar-inverse")
  val nav_staticTop = key("navbar-static-top")
  val nav_pills = key("nav-pills")
  val navbar = key("navbar-nav")
  val navbar_form = key("navbar-form")
  val navbar_right = key("navbar-right")
  val navbar_left = key("navbar-left")

  val dropdown = key("dropdown")

  //Inputs
  def input(content: String) = tags.input(`class` := "form-control", value := content)

  def checkbox(default: Boolean) = tags.input(`type` := "checkbox", checked := default)

  // Label
  def label(content: String, keys: ClassKeyAggregator = emptyCK): TypedTag[HTMLSpanElement] = span("label " + keys.key)(content)

  val label_default = key("label-default")
  val label_primary = key("label-primary")
  val label_success = key("label-success")
  val label_info = key("label-info")
  val label_warning = key("label-warning")
  val label_danger = key("label-danger")
  val black_label = key("black-label")

  //Select (to be used with button class aggregators )
  def select(id: String, contents: Seq[(String, String)], key: ClassKeyAggregator) = buttonGroup()(
    tags.a(
      `class` := "btn " + key.key + " dropdown-toggle", data("toggle") := "dropdown", href := "#"
    )("Select", span("caret")),
    ul(`class` := "dropdown-menu")(
      for (c ← contents) yield {
        tags.li(tags.a(
          href := "#")(c._2)
        )
      }
    )
  )

  def glyph(key: ClassKeyAggregator, onclickAction: () ⇒ Unit = () ⇒ {}): TypedTag[HTMLSpanElement] =
    span("glyphicon " + key.key)(aria.hidden := "true", onclick := { () ⇒ onclickAction() })

  val glyph_edit = "glyphicon-pencil"
  val glyph_trash = "glyphicon-trash"
  val glyph_plus = "glyphicon-plus"
  val glyph_plus_sign = "glyphicon-plus-sign"
  val glyph_minus_sign = "glyphicon-minus-sign"
  val glyph_minus = "glyphicon-minus"
  val glyph_ok = "glyphicon-ok"
  val glyph_question = "glyphicon-question-sign"
  val glyph_file = "glyphicon-file"
  val glyph_folder_close = "glyphicon-folder-close"
  val glyph_home = "glyphicon-home"
  val glyph_upload = "glyphicon-cloud-upload"
  val glyph_download = "glyphicon-cloud-download"
  val glyph_download_alt = "glyphicon-download-alt"
  val glyph_settings = "glyphicon-cog"
  val glyph_off = "glyphicon-off"
  val glyph_flash = "glyphicon-flash"
  val glyph_flag = "glyphicon-flag"
  val glyph_remove = "glyphicon-remove-sign"
  val glyph_road = "glyphicon-road"
  val glyph_fire = "glyphicon-fire"
  val glyph_list = "glyphicon-list"
  val glyph_stats = "glyphicon-stats"
  val glyph_refresh = "glyphicon-refresh"
  val glyph_lock = "glyphicon-lock"
  val glyph_archive = "glyphicon-compressed"
  val glyph_market = "glyphicon-shopping-cart"
  val glyph_info = "glyphicon-info-sign"
  val glyph_plug = "icon-plug"
  val glyph_exclamation = "glyphicon-exclamation-sign"

  //Button
  def button(content: String, keys: ClassKeyAggregator, todo: () ⇒ Unit = () ⇒ {}): TypedTag[HTMLButtonElement] =
    tags.button(`class` := ("btn " + keys.key), `type` := "button", onclick := { () ⇒ todo() })(content)

  def button(content: TypedTag[HTMLElement], keys: ClassKeyAggregator): TypedTag[HTMLButtonElement] =
    tags.button(`class` := ("btn " + keys.key), `type` := "button")(content)

  def button(content: String): TypedTag[HTMLElement] = button(content, btn_default)

  def button(content: TypedTag[HTMLElement]): TypedTag[HTMLButtonElement] = button(content, btn_default)(span(" "))

  def glyphButton(text: String, buttonCB: ClassKeyAggregator, glyCA: ClassKeyAggregator, todo: () ⇒ Unit): TypedTag[HTMLSpanElement] =
    span("btn " + buttonCB.key)(cursor := "pointer", `type` := "button")(glyph(glyCA))(text)(onclick := {
      () ⇒ todo()
    })

  def waitingSpan(text: String, buttonCB: ClassKeyAggregator): TypedTag[HTMLSpanElement] =
    //<a href="#" class="btn btn-info btn-lg"><span class="glyphicon glyphicon-search"></span> Search</a>
    span("btn " + buttonCB.key)(
      span("loading")(text)
    )

  def glyphBorderButton(text: String,
                        buttonCB: ClassKeyAggregator,
                        glyCA: ClassKeyAggregator, todo: () ⇒ Unit): TypedTag[HTMLButtonElement] = {
    tags.button(`type` := "button", `class` := "btn " + buttonCB.key, onclick := { () ⇒ todo() })(
      tags.span(aria.hidden := true)(glyph(glyCA))
    )
  }

  def glyphButton(glyCA: ClassKeyAggregator, todo: () ⇒ Unit): TypedTag[HTMLSpanElement] = glyphButton("", emptyCK, glyCA, todo)

  def glyphSpan(glyCA: ClassKeyAggregator, todo: () ⇒ Unit, linkName: String = ""): TypedTag[HTMLSpanElement] =
    tags.span(cursor := "pointer", glyph(glyCA)(linkName)(onclick := { () ⇒
      todo()
    }))

  def fileInputMultiple(todo: HTMLInputElement ⇒ Unit) = {
    lazy val input: HTMLInputElement = tags.input(id := "fileinput", `type` := "file", multiple := "")(onchange := { () ⇒
      todo(input)
    }).render
    input
  }

  def fileInput(todo: HTMLInputElement ⇒ Unit) = {
    lazy val input: HTMLInputElement = tags.input(id := "fileinput", `type` := "file")(onchange := { () ⇒
      todo(input)
    }).render
    input
  }

  def uploadButton(todo: HTMLInputElement ⇒ Unit): TypedTag[HTMLSpanElement] = {
    span(" btn-file")(cursor := "pointer", id := "success-like")(
      glyph(glyph_upload),
      fileInputMultiple(todo)
    )
  }

  def uploadButton2(todo: HTMLInputElement ⇒ Unit): TypedTag[HTMLSpanElement] =
    span("btn-file")(
      glyph(glyph_upload)(fileInput(todo))
    )

  def uploadGlyphSpan(todo: HTMLInputElement ⇒ Unit): TypedTag[HTMLSpanElement] =
    span("btn-file")(
      glyph(glyph_upload),
      fileInputMultiple(todo)
    )

  def progressBar(barMessage: String, ratio: Int): TypedTag[HTMLDivElement] =
    div("progress")(
      div("progress-bar")(width := ratio.toString() + "%")(
        barMessage
      )
    )

  val btn_default = key("btn-default")
  val btn_primary = key("btn-primary")
  val btn_success = key("btn-success")
  val btn_info = key("btn-info")
  val btn_warning = key("btn-warning")
  val btn_danger = key("btn-danger")
  val btn_large = key("btn-lg")
  val btn_medium = key("btn-md")
  val btn_small = key("btn-sm")
  val btn_test = key("myButton")
  val btn_right = key("pull-right")

  // Badges
  def badge(content: String, badgeValue: String, keys: ClassKeyAggregator = emptyCK) =
    button(content + " ", keys)(span("badge")(badgeValue))

  //Button group
  def buttonGroup(keys: ClassKeyAggregator = emptyCK) = div("btn-group")

  val btn_group_large = key("btn-group-lg")
  val btn_group_medium = key("btn-group-sm")
  val btn_group_small = key("btn-group-xs")

  def buttonToolBar = div("btn-toolbar")(role := "toolbar")

  //Modalg Dialog
  type Dialog = TypedTag[HTMLDivElement]
  type ModalID = String

  def modalDialog(ID: ModalID, typedTag: TypedTag[_]*): Dialog =
    div("modal fade")(id := ID,
      div("modal-dialog")(
        div("modal-content")(
          typedTag)
      )
    )

  def headerDialog = div("modal-header modal-info")

  def bodyDialog = div("modal-body")

  def footerDialog = div("modal-footer")

  //Jumbotron
  def jumbotron(modifiers: scalatags.JsDom.Modifier*) =
    div("container theme-showcase")(role := "main")(
      div("jumbotron")(
        p(
          (modifiers.toSeq: _*)
        )
      )
    )

  object ScrollableTextArea {

    sealed trait AutoScroll

    case class TopScroll() extends AutoScroll

    case class BottomScroll() extends AutoScroll

    case class NoScroll(scrollHeight: Int) extends AutoScroll

  }

  import ScrollableTextArea._

  //TextArea
  def textArea(nbRow: Int) = tags.textarea(`class` := "form-control", rows := nbRow)

  def scrollableText(text: String = "", scrollMode: AutoScroll = TopScroll()): ScrollableText = ScrollableText(text, scrollMode)

  def scrollableDiv(element: Div = tags.div.render, scrollMode: AutoScroll = BottomScroll()): ScrollableDiv = ScrollableDiv(element, scrollMode)

  trait Scrollable {

    def scrollMode: Var[AutoScroll]

    def sRender: HTMLElement

    def view: HTMLElement = tags.div(sRender).render

    def setScrollMode = {
      val scrollHeight = sRender.scrollHeight
      val scrollTop = sRender.scrollTop.toInt
      scrollMode() =
        if ((scrollTop + sRender.offsetHeight.toInt) > scrollHeight) BottomScroll()
        else NoScroll(scrollTop)
    }

    def doScroll = scrollMode() match {
      case b: BottomScroll ⇒ sRender.scrollTop = sRender.scrollHeight
      case n: NoScroll     ⇒ sRender.scrollTop = n.scrollHeight
      case _               ⇒
    }
  }

  case class ScrollableText(initText: String, _scrollMode: AutoScroll) extends Scrollable {
    //FIXME: ADD FEATURE: SET THE NUMBER OF LINES IN VIEW (DEFAULT IS 500)
    val scrollMode: Var[AutoScroll] = Var(_scrollMode)
    val tA = textArea(20)(initText, onscroll := { (e: Event) ⇒ setScrollMode })
    val sRender = tA.render

    def setContent(out: String) = {
      sRender.value = out
    }
  }

  case class ScrollableDiv(_element: Div, _scrollMode: AutoScroll) extends Scrollable {
    //FIXME: ADD FEATURE: SET THE NUMBER OF LINES IN VIEW (DEFAULT IS 500)
    val scrollMode: Var[AutoScroll] = Var(_scrollMode)
    val child: Var[Node] = Var(tags.div)
    val tA = div("scrollable")(Rx {
      child()
    }, onscroll := { (e: Event) ⇒ setScrollMode })

    def setChild(d: Div) = child() = d

    val sRender = tA.render

  }

  //table
  def table(keys: ClassKeyAggregator) = tags.table(`class` := keys.key)

  def tr(keys: ClassKeyAggregator) = tags.tr(`class` := keys.key)

  def th(keys: ClassKeyAggregator) = tags.th(`class` := keys.key)

  def td(keys: ClassKeyAggregator) = tags.td(`class` := (keys + key("vert-align")).key)

  val bordered = key("table table-bordered")
  val striped = key("table table-striped")
  val active = key("active")
  val success = key("success")
  val danger = key("danger")
  val warning = key("warning")
  val info = key("info")
  val nothing = key("")

  //Forms
  def form(keys: ClassKeyAggregator = emptyCK) = tags.form(`class` := keys.key)

  def formGroup(keys: ClassKeyAggregator = emptyCK) = div("form-group ")

  val large_form_group = key("form-group-lg")
  val small_form_group = key("form-group-sm")
  val form_inline = key("form-inline")

  //Input group
  def inputGroup(keys: ClassKeyAggregator = emptyCK) = div(key("input-group") + keys.key)

  def inputGroupButton = span("input-group-btn")

  def inputGroupAddon = span("input-group-addon")

  val input_group_lg = "input-group-lg"

  //Grid
  val row = key("row")
  val col_md_1 = key("col-md-1")
  val col_md_2 = key("col-md-2")
  val col_md_3 = key("col-md-3")
  val col_md_4 = key("col-md-4")
  val col_md_5 = key("col-md-5")
  val col_md_6 = key("col-md-6")
  val col_md_7 = key("col-md-7")
  val col_md_8 = key("col-md-8")
  val col_md_12 = key("col-md-12")

  val col_md_offset_3 = key("col-md-offset-3")
  val col_md_offset_2 = key("col-md-offset-2")

  def labeledField(labelString: String, element: HTMLElement) = {
    val ID = uuID
    form("form-horizontal")(
      div("control-group")(
        label(labelString, "control-label")(`for` := ID),
        div("controls")(
          tags.div(id := ID)(element)
        )
      )
    )
  }

  //Misc
  val center = key("text-center")
  val spacer20 = key("spacer20")

  def panel(heading: String, bodyElement: TypedTag[HTMLElement]) =
    div("panel panel-default")(
      div("panel-heading")(heading),
      div("panel-body")(bodyElement.render)
    )

  val alert_warning = key("alert-warning")

  def alert(alertType: ClassKeyAggregator, content: String, todook: () ⇒ Unit, todocancel: () ⇒ Unit) =
    div("alertSettings alert " + alertType.key)(role := "alert")(
      content,
      div("spacer20")(
        buttonGroup("left")(
          button("OK", btn_danger, todook),
          button("Cancel", btn_default, todocancel)
        )
      )
    )

}
