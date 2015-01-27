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

import fr.iscpif.scaladget.mapping.{ Select2QueryOptions, Select2Options }
import fr.iscpif.scaladget.mapping.Select2Utils._
import org.scalajs.dom
import org.scalajs.dom._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js
import scalatags.JsDom.TypedTag

import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._

import org.scalajs.jquery.jQuery
import scala.scalajs.js
import rx._
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.js

@JSExport("Forms")
object Forms {

  implicit def stringToClassKeyAggregator(s: String): ClassKeyAggregator = key(s)

  implicit def formTagToNode(tt: HtmlTag): org.scalajs.dom.Node = tt.render

  implicit class BootstrapTypedTag[+Output <: dom.Element](t: TypedTag[Output]) {
    def +++(m: Seq[Modifier]) = t.copy(modifiers = t.modifiers :+ m.toSeq)
  }

  def emptyCK = ClassKeyAggregator.empty

  def key(s: String) = new ClassKeyAggregator(s)

  //Div
  def div(keys: ClassKeyAggregator = emptyCK) = tags.div(`class` := keys.key)

  //span
  def span(keys: ClassKeyAggregator = emptyCK) = tags.span(`class` := keys.key)

  // Nav
  class NavItem(val navid: String, content: String, val todo: () ⇒ Unit = () ⇒ {}, active: Boolean = false) {
    val activeString = { if (active) "active" else "" }
    def render = li(role := "presentation", id := navid, `class` := activeString)(a(href := "#")(content))
  }

  def navItem(id: String, content: String, todo: () ⇒ Unit = () ⇒ {}, active: Boolean = false) = new NavItem(id, content, todo, active)

  def nav(uuid: String, contents: Seq[(TypedTag[HTMLLIElement], String, () ⇒ Unit)], keys: ClassKeyAggregator): TypedTag[HTMLElement] =
    ul(`class` := "nav " + keys.key, id := uuid, role := "tablist")(
      contents.map { c ⇒
        c._1(scalatags.JsDom.attrs.onclick := { () ⇒
          jQuery("#" + uuid + " .active").removeClass("active")
          jQuery("#" + c._2).addClass("active")
          c._3()
        })
      }: _*)

  def nav(uuid: String, keys: ClassKeyAggregator, contents: NavItem*): TypedTag[HTMLElement] =
    nav(uuid, contents.map { c ⇒ (c.render, c.navid, c.todo) }, keys)

  val nav_default = "navbar-default"
  val nav_inverse = "navbar-inverse"
  val nav_staticTop = "navbar-static-top"
  val nav_pills = "nav-pills"
  val navbar = "navbar-nav"
  val navbar_form = "navbar-form"
  val navbar_right = "navbar-right"
  val navbar_left = "navbar-left"

  val dropdown = "dropdown"

  //Input
  def input(content: String = "") = tags.input(content, `class` := "form-control")

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
    a(
      `class` := "btn " + key.key + " dropdown-toggle", "data-toggle".attr := "dropdown", href := "#"
    )("Select", span("caret")),
    ul(`class` := "dropdown-menu")(
      for (c ← contents) yield {
        tags.li(a(
          href := "#")(c._2)
        )
      }
    )
  )

  def glyph(key: ClassKeyAggregator): TypedTag[HTMLSpanElement] =
    span("glyphicon " + key.key)("aria-hidden".attr := "true")

  val glyph_edit = "glyphicon-pencil"
  val glyph_trash = "glyphicon-trash"
  val glyph_plus = "glyphicon-plus"
  val glyph_ok = "glyphicon-ok"

  //Button
  def button(content: String, keys: ClassKeyAggregator): TypedTag[HTMLButtonElement] =
    tags.button(`class` := ("btn " + keys.key), `type` := "button")(content)

  def button(content: TypedTag[HTMLElement], keys: ClassKeyAggregator): TypedTag[HTMLButtonElement] =
    tags.button(`class` := ("btn " + keys.key), `type` := "button")(content)

  def button(content: String): TypedTag[HTMLElement] = button(content, btn_default)

  def button(content: TypedTag[HTMLElement]): TypedTag[HTMLButtonElement] = button(content, btn_default)(span(" "))

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
  def modalDialog(ID: String, header: TypedTag[HTMLFormElement], body: TypedTag[HTMLDivElement], footer: TypedTag[HTMLElement]) =
    new ModalDialog(ID, header, body, footer)

  def modalDialog(ID: String, typedTag: TypedTag[_]*) =
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

  //table
  def table = tags.table(`class` := "table table-stripped")
  def tr(keys: ClassKeyAggregator = emptyCK) = tags.tr(`class` := keys.key)
  def td(keys: ClassKeyAggregator = emptyCK) = tags.td(`class` := keys.key)

  //Forms
  def form(keys: ClassKeyAggregator = emptyCK) = tags.form(`class` := keys.key)
  def formGroup = div("form-group ")

  val large_form_group = key("form-group-lg")
  val small_form_group = key("form-group-sm")
  val form_inline = key("form-inline")

  //Input group
  def inputGroup(keys: ClassKeyAggregator) = div(key("input-group") + keys.key)

  def inputGroupButton = span("input-group-btn")
  val input_group_lg = "input-group-lg"

  //Grid
  val row = key("row")
  val col_md_1 = key("col-md-1")
  val col_md_2 = key("col-md-2")
  val col_md_5 = key("col-md-5")
  val col_md_6 = key("col-md-6")
}
