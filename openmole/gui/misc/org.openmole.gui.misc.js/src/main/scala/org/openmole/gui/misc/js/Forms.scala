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

import scalatags.JsDom.all._

import org.scalajs.jquery.jQuery
import scala.scalajs.js
import js.Dynamic.{ literal ⇒ lit }
import rx._
import org.openmole.gui.misc.js.JsRxTags._
import org.scalajs.dom
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
  def d[T <: TypedTag[HTMLDivElement]](t: T*): TypedTag[HTMLDivElement] = scalatags.JsDom.tags.div(t.toSeq: _*)

  // def dd(m: Modifier*) = div.copy(modifiers = div.modifiers :+ m.toSeq)
  def dd(m: Modifier*) = div(m.toSeq)

  // Nav
  def nav(keys: ClassKeyAggregator, navItems: TypedTag[HTMLElement]*): TypedTag[HTMLElement] = ul(`class` := keys.key, role := "tablist")(navItems.toSeq: _*)

  private val navPrefix = key("nav")
  val nav_default = navPrefix + "navbar-default"
  val nav_inverse = navPrefix + "navbar-inverse"
  val nav_staticTop = navPrefix + "navbar-static-top"
  val nav_pills = navPrefix + "nav-pills"

  // Nav item
  def navItem(content: String, keys: ClassKeyAggregator = emptyCK): TypedTag[HTMLElement] =
    li(role := "presentation")(a(href := "#")(content))

  val dropdown = "dropdown"

  //Input
  def input(content: String) = scalatags.JsDom.tags.input(content, `class` := "form-control")

  // Label
  def label(content: String, keys: ClassKeyAggregator = emptyCK): TypedTag[HTMLSpanElement] = span(`class` := ("label " + keys.key))(content)

  val label_default = key("label-default")
  val label_primary = key("label-primary")
  val label_success = key("label-success")
  val label_info = key("label-info")
  val label_warning = key("label-warning")
  val label_danger = key("label-danger")

  //Select (to be used with button class aggregators )
  def select(key: ClassKeyAggregator, contents: Seq[(String, String)]) =
    scalatags.JsDom.tags.select(`class` := "selectpicker", dataWith("style") := key.key)(
      for (c ← contents) yield { scalatags.JsDom.tags.option(value := c._1)(c._2) }
    )

  def glyph(key: ClassKeyAggregator): TypedTag[HTMLSpanElement] =
    span(`class` := "glyphicon " + key.key, ariaWith("hidden") := "true")

  val glyph_edit = "glyphicon-pencil"
  val glyph_trash = "glyphicon-trash"
  val glyph_plus = "glyphicon-plus"
  val glyph_ok = "glyphicon-ok"

  //Button
  def button(content: String, keys: ClassKeyAggregator): TypedTag[HTMLButtonElement] =
    scalatags.JsDom.tags.button(`class` := ("btn " + keys.key), `type` := "button")(content)

  def button(content: TypedTag[HTMLElement], keys: ClassKeyAggregator): TypedTag[HTMLButtonElement] =
    scalatags.JsDom.tags.button(`class` := ("btn " + keys.key), `type` := "button")(content)

  def button(content: String): TypedTag[HTMLElement] = button(content, btn_default)

  def button(content: TypedTag[HTMLElement]): TypedTag[HTMLButtonElement] = button(content, btn_default)

  private val btnPrefix = key("btn")
  val btn_default = key("btn-default")
  val btn_primary = key("btn-primary")
  val btn_success = key("btn-success")
  val btn_info = key("btn-info")
  val btn_warning = key("btn-warning")
  val btn_danger = key("btn-danger")
  //  val btn_dropdown = dropdownComponent(btnPrefix)
  val btn_large = key("btn-lg")
  val btn_medium = key("btn-md")
  val btn_small = key("btn-sm")

  // Badges
  def badge(content: String, badgeValue: String, keys: ClassKeyAggregator = emptyCK) =
    button(content, keys)(span(`class` := "badge", badgeValue))

  //Button group
  def buttonGroup(keys: ClassKeyAggregator = emptyCK) = div(`class` := "btn-group")
  val btn_group_large = key("btn-group-lg")
  val btn_group_medium = key("btn-group-sm")
  val btn_group_small = key("btn-group-xs")

  def buttonToolBar = div(`class` := "btn-toolbar", role := "toolbar")

  def modalDialog(ID: String, header: TypedTag[HTMLFormElement], body: TypedTag[HTMLDivElement], footer: TypedTag[HTMLElement]) =
    new ModalDialog(ID, header, body, footer)

  def jumbotron(modifiers: scalatags.JsDom.Modifier*) =
    div(`class` := "container theme-showcase", role := "main")(
      div(`class` := "jumbotron")(
        p(
          (modifiers.toSeq: _*)
        )
      )
    )

  //table
  def table = scalatags.JsDom.tags.table(`class` := "table table-stripped")

  //Forms
  def formGroup = div(`class` := "form-group ")
  def formLine = div(`class` := "form-inline")

  val large_form_group = key("form-group-lg")
  val small_form_group = key("form-group-sm")

}