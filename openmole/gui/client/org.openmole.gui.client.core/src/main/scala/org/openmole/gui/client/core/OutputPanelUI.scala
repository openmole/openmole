package org.openmole.gui.client.core

/*
 * Copyright (C) 30/01/15 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.service.dataui.OutputDataUI
import org.openmole.gui.ext.dataui.PanelUI
import scalatags.JsDom.tags
import org.openmole.gui.misc.js.{ Forms ⇒ bs }

import scala.scalajs.js.annotation.JSExport

class OutputPanelUI(panel: GenericPanel, dataUI: OutputDataUI) extends PanelUI {

  @JSExport
  val view = tags.div()

  def save = {
    //dataUI.truc() = trucInput.value
  }
}
