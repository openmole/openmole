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

import fr.iscpif.scaladget.mapping.Select2Options
import fr.iscpif.scaladget.mapping.Select2Utils._
import org.openmole.gui.ext.aspects.{ Identifiable, Displayable }
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js
import scalatags.JsDom.tags._
import scalatags.JsDom.tags2._
import scalatags.JsDom.attrs._
import scalatags.JsDom.short._
import org.scalajs.jquery.jQuery
import scalatags.generic.TypedTag
import fr.iscpif.scaladget.select2._
import scala.scalajs.js
import js.Dynamic.{ literal ⇒ lit }
import rx._
import org.openmole.gui.misc.js.JsRxTags._

@JSExport("Forms")
object Forms {

  //type FormTag = TypedTag[dom.Element, dom.Element, dom.Node]

  implicit def stringToClassKeyAggregator(s: String): ClassKeyAggregator = key(s)

  implicit def typedTagToNode[T <: org.scalajs.dom.Element](tt: scalatags.JsDom.TypedTag[T]): org.scalajs.dom.Node = tt.render

  implicit def formTagToNode(tt: HtmlTag): org.scalajs.dom.Node = tt.render

  def emptyCK = ClassKeyAggregator.empty

  def key(s: String) = new ClassKeyAggregator(s)

  //Div
  def d[T <: HtmlTag](t: T*): HtmlTag = scalatags.JsDom.tags.div(t.toSeq: _*)

  // Nav
  def nav(keys: ClassKeyAggregator, navItems: HtmlTag*) = ul(`class` := keys.key, role := "tablist")(navItems.toSeq: _*)

  private val navPrefix = key("nav")
  val nav_default = navPrefix + "navbar-default"
  val nav_inverse = navPrefix + "navbar-inverse"
  val nav_staticTop = navPrefix + "navbar-static-top"
  val nav_pills = navPrefix + "nav-pills"

  // Nav item
  def navItem(content: String): HtmlTag = navItem(content, emptyCK)

  def navItem(content: String, modifiers: scalatags.JsDom.Modifier*): HtmlTag = navItem(content, emptyCK, modifiers.toSeq: _*)

  def navItem(content: String, keys: ClassKeyAggregator, modifiers: scalatags.JsDom.Modifier*): HtmlTag =
    li(role := "presentation")(a(href := "#")(content))(modifiers.toSeq: _*)

  val dropdown = "dropdown"

  // Label
  def label(content: String, keys: ClassKeyAggregator, modifiers: scalatags.JsDom.Modifier*): HtmlTag = span(`class` := ("label" + keys.key))(content)(modifiers.toSeq: _*)

  def label(content: String, modifiers: scalatags.JsDom.Modifier*): HtmlTag = label(content, emptyCK, modifiers.toSeq: _*)

  val label_default = key("label-default")
  val label_primary = key("label-primary")
  val label_success = key("label-success")
  val label_info = key("label-info")
  val label_warning = key("label-warning")
  val label_danger = key("label-danger")

  //Button
  def button(content: String, keys: ClassKeyAggregator, modifiers: scalatags.JsDom.Modifier*): HtmlTag =
    scalatags.JsDom.tags.button(`class` := ("btn " + keys.key), `type` := "button")(content).apply(modifiers.toSeq: _*)

  def button(content: String, modifiers: scalatags.JsDom.Modifier*): HtmlTag = button(content, btn_default, modifiers.toSeq: _*)

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
  def badge(content: String, badgeValue: String): HtmlTag = badge(content, badgeValue, emptyCK)

  def badge(content: String, badgeValue: String, keys: ClassKeyAggregator, modifiers: scalatags.JsDom.Modifier*): HtmlTag =
    button(content, keys, span(`class` := "badge", badgeValue) +: modifiers.toSeq: _*)

  //Button group
  def buttonGroup = div(`class` := "btn-group")

  def modalDialog(ID: String, header: Var[HtmlTag], body: Var[HtmlTag], footer: Var[HtmlTag]): HtmlTag = {
    println("modalDialog")

    div(`class` := "modal fade", id := ID)(
      div(`class` := "modal-dialog")(
        div(`class` := "modal-content")(
          // parts.map { p ⇒ p() }.toSeq: _*
          //Header
          div(`class` := "modal-header")(
            button("", `class` := "close", dataWith("dismiss") := "modal")(
              span(ariaWith("hidden") := "true", "x"),
              span(`class` := "sr-only", "Close")
            ),
            header()
          ),
          //Body
          Rx {
            //  println("writing body ...")
            div(`class` := "modal-body")(body) //
          },
          //Footer
          div(`class` := "modal-footer")(footer())
        )
      )
    )

  }

  def jumbotron(modifiers: scalatags.JsDom.Modifier*): HtmlTag =
    div(`class` := "container theme-showcase", role := "main")(
      div(`class` := "jumbotron")(
        p(
          (modifiers.toSeq: _*)
        )
      )
    )

  /* def modalHeader(tag: HtmlTag): Var[HtmlTag] = Var(div(`class` := "modal-header")(
    button("", `class` := "close", dataWith("dismiss") := "modal")(
      span(ariaWith("hidden") := "true", "x"),
      span(`class` := "sr-only", "Close")
    ),
    tag
  ))

  def modalBody(tag: Var[HtmlTag]): Var[HtmlTag] = Rx {
    println("modal body !")
    div(`class` := "modal-body")(tag())
  }()

  def modalFooter(tag: HtmlTag): Var[HtmlTag] = Var(div(`class` := "modal-footer")(tag))*/

  def autoinput[T <: Displayable with Identifiable](autoID: String, placeHolder: String, contents: Seq[T]) = {
    new AutoInput[T](autoID, placeHolder, contents)
    /*
    val mapping = contents.map { c ⇒ c.id -> c }.toMap

    select(id := "e1")(
      contents.map { c ⇒
        option(value := c.id)(c.name)
      }.toSeq: _*
    )*/

  }

  @JSExport
  protected def select2(): Unit = {
    jQuery(() ⇒ jQuery("#factoryUI").select2(lit(placeholder = "Yoyo")))
    jQuery(() ⇒ jQuery("#dataUI").select2(lit(placeholder = "Yoyo")))
  }

}