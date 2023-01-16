package org.openmole.gui.client.core

import scaladget.bootstrapnative.bsn._

import com.raquo.laminar.api.L._
import org.openmole.gui.client.ext._

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

class TextPanel(title: String) {

  val content: Var[String] = Var("")

  def open = dialog.show

  def close = dialog.hide

  lazy val dialog: ModalDialog = ModalDialog(
    span(b(title)),
    textArea(child.text <-- content.signal),
    closeButton("Close", () ⇒ close),
    omsheet.panelWidth(65),
    () ⇒ {},
    () ⇒ {}
  )

}