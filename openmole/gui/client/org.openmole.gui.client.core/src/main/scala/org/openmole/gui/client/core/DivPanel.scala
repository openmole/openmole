package org.openmole.gui.client.core

import org.scalajs.dom.raw.HTMLDivElement

import scalatags.JsDom.{ TypedTag, tags }
import scalatags.JsDom.all._
import scaladget.bootstrapnative.bsn._

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

class DivPanel(title: String, div: TypedTag[HTMLDivElement]) {

  def open = dialog.show

  val dialog = ModalDialog()

  dialog.header(
    tags.span(tags.b(title))
  )

  dialog.body(div)

  dialog.footer(ModalDialog.closeButton(dialog, btn_default, "Close"))

}