package org.openmole.gui.client.core

import scalatags.JsDom.tags
import scalatags.JsDom.all._
import scaladget.api.{ BootstrapTags ⇒ bs }
import scaladget.stylesheet.all
import rx._
import org.openmole.gui.ext.tool.client.JsRxTags._
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

  val content: Var[String] = Var("")

  def open = dialog.show

  val dialog = bs.ModalDialog(omsheet.panelWidth(65))

  dialog.header(
    tags.span(tags.b(title))
  )

  val textArea = bs.scrollableText("")

  content.trigger {
    println("Trigger " + content.now)
    textArea.setContent(content.now)
  }

  dialog.body(div(
    textArea.sRender
  ))

  dialog.footer(bs.ModalDialog.closeButton(dialog, all.btn_default, "Close"))

}