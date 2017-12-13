package org.openmole.gui.client.core

import scalatags.JsDom.tags
import scalatags.JsDom.all._
import scaladget.bootstrapnative.bsn._

import rx._
import org.openmole.gui.ext.tool.client._

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

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val content: Var[String] = Var("")

  def open = dialog.show

  val dialog = ModalDialog(omsheet.panelWidth(65))

  dialog.header(
    tags.span(tags.b(title))
  )

  val textArea = scrollableText("")

  content.trigger {
    println("Trigger " + content.now)
    textArea.setContent(content.now)
  }

  dialog.body(div(
    textArea.sRender
  ))

  dialog.footer(ModalDialog.closeButton(dialog, btn_default, "Close"))

}