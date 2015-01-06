package org.openmole.gui.misc.js

/*
 * Copyright (C) 29/12/14 // mathieu.leclaire@openmole.org
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

import org.scalajs.dom.HTMLElement
import org.scalajs.jquery.jQuery

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import org.scalajs.dom
import org.openmole.gui.misc.js.JsRxTags._
import fr.iscpif.scaladget.mapping.Select2Utils._
import rx._

class ModalDialog(ID: String, val header: TypedTag[HTMLElement], val body: Var[TypedTag[HTMLElement]], val footer: TypedTag[HTMLElement]) {

  val content =
    div(`class` := "modal-content",
      div(`class` := "modal-header")(header),
      Rx {
        div(`class` := "modal-body", body())
      },
      div(`class` := "modal-footer")(footer)

    )

  val shell = div(`class` := "modal fade", id := ID,
    div(`class` := "modal-dialog",
      content
    )
  )

  /*jQuery(jQid).on("hide.bs.modal", { () ⇒
    {
      println("in hide.bs.modal reove")
      jQuery(jQid).remove()
    }
  })*/

  def jQid = "#" + ID

}
