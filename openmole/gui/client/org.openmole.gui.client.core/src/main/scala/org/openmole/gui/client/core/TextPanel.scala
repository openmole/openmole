package org.openmole.gui.client.core

import org.openmole.gui.misc.js.BootstrapTags.ModalID
import rx.core.Var
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import bs._
import rx._

/*
 * Copyright (C) 03/08/15 // mathieu.leclaire@openmole.org
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

class TextPanel(lazy val modalID: ModalID, title: String) extends ModalPanel {

  val content: Var[String] = Var("")

  def onOpen() = {}

  def onClose() = {}

  val dialog = bs.modalDialog(modalID,
    headerDialog(
      tags.span(tags.b(title))
    ),
    bodyDialog(
      tags.div(Rx {
        bs.textArea(30)(content())
      }
      )),
    footerDialog(closeButton)
  )

}