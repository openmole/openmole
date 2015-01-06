package org.openmole.gui.misc.js

/*
 * Copyright (C) 05/12/14 // mathieu.leclaire@openmole.org
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
import org.scalajs.dom.{ HTMLSelectElement }
import scala.scalajs.js.annotation.JSExport
import fr.iscpif.scaladget.mapping.Select2Utils._
import org.openmole.gui.misc.js.JsRxTags._
import fr.iscpif.scaladget.select2._
import scala.scalajs.js
import js.Dynamic.{ literal ⇒ lit }
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import org.scalajs.jquery.jQuery
import rx._

abstract class GenericAutoInput[T](autoID: String, val contents: Var[Seq[T]], default: Option[T] = None, placeHolder: Option[String]) {

  def jQid = "#" + autoID

  val selector: Rx[TypedTag[_ <: HTMLSelectElement]]

  lazy val content: Var[Option[T]] = Var(contents().size match {
    case 0 ⇒ None
    case _ ⇒ default match {
      case None ⇒ Some(contents()(0))
      case _ ⇒
        val ind = contents().indexOf(default.get)
        if (ind != -1) Some(contents()(ind)) else Some(contents()(0))
    }
  })

  def applyOnChange: Unit = {
    content() = Some(contents()(jQuery(jQid).find("option:selected").index()))
  }

  def setDefault(d: T) = {
    val ind = contents().indexOf(d)
    content() = Some(contents()(if (ind != -1) ind else 0))
  }

  //  def reload = jQuery(jQid).select2("destroy").select2(jQuery(jQid).data("modal", null))
}

class AutoSelect[T <: DisplayableRx with Identifiable](autoID: String, data: Var[Seq[T]], default: Option[T] = None, placeHolder: Option[String])
    extends GenericAutoInput[T](autoID, data, default, placeHolder) {

  type T = DisplayableRx with Identifiable
  val selector = Rx {
    select(id := autoID,
      placeholder := placeHolder.getOrElse(""),
      onchange := { () ⇒ applyOnChange }
    )(contents().map { c ⇒
        option(value := c.uuid)(c.name())
      }.toSeq: _*
      )
  }
  /* Rx {
     println("In select RX")
     selector()(contents().map { c ⇒
       option(value := c.uuid)(c.name())
     }.toSeq: _*
     )
   }*/

  def contentName = content().map {
    _.name()
  }.getOrElse("")

  def update(data: Seq[(String, String)]) = selector()
}

/*
class AutoInput[T <: DisplayableRx with Identifiable](autoID: String, contents: Seq[T], default: Option[T] = None, placeHolder: Option[String])
    extends GenericAutoInput[T](autoID, contents, default, placeHolder) {

  val myinput = input()
  val myselect = select(id := autoID,
    placeholder := placeHolder.getOrElse(""),
    onchange := { () ⇒ applyOnChange }
  // onsubmit := { () ⇒ applyOnSubmit }
  )(
      contents.map { c ⇒
        option(value := c.uuid)(c.name())
      }.toSeq: _*
    )

  val selector = Var(
    div(myselect, myinput)
  )

  def inputValue = myinput.render.value

  //FIXME: the keydown event should be implemented in select2 4.0
  /*jQuery(jQid).on("keydown", "input", (e: KeyboardEvent) ⇒ {
    println("In applyOnSubmit")
    if (e.keyCode == 13) {
      println("Enter !")
      myinput.render.value = myselect.render.value
      selector() = div(myinput)
    }
  }
  )

  def applyOnSubmit: Unit = {
    println("In applyOnSubmit")
    myinput.render.value = myselect.render.value
    selector() = div(myinput)
  }*/
}*/ 