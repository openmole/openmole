package org.openmole.gui.client.core

import org.scalajs.dom.raw.HTMLDivElement
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import bs._
/*
 * Copyright (C) 26/08/15 // mathieu.leclaire@openmole.org
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

class DivPanel(_modalID: ModalID, title: String, div: HTMLDivElement) extends ModalPanel {

  lazy val modalID = _modalID

  def onOpen() = {}

  def onClose() = {}

  val dialog = bs.modalDialog(
    modalID,
    headerDialog(
      tags.span(tags.b(title))
    ),
    bodyDialog(div),
    footerDialog(closeButton)
  )

}