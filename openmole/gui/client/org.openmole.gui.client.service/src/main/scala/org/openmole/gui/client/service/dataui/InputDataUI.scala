package org.openmole.gui.client.service.dataui

import org.openmole.gui.ext.data._
import org.openmole.gui.ext.dataui._
import org.openmole.gui.ext.data.ProtoTYPE._
import rx._

/*
 * Copyright (C) 28/01/15 // mathieu.leclaire@openmole.org
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

class InputDataUI(val ioMapping: Boolean) extends DataUI {
  type DATA = InputData

  def dataType = "Inputs"

  def +=(inputUI: InputUI) =
    if (!exists(inputUI))
      inputsUI() = inputUI +: inputsUI()

  def -=(inputUI: InputUI) = inputsUI() = inputsUI().filter {
    _.id != inputUI.id
  }

  def exists(inputUI: InputUI) = inputsUI().exists {
    _.id == inputUI.id
  }

  val inputsUI: Var[Seq[InputUI]] = Var(Seq())

  def panelUI = new InputPanelUI(this)

  def data = new InputData {
    def inputs = inputsUI().map { id ⇒
      Input(id.protoDataBagUI.dataUI().data, id.default(), id.mapping())
    }
  }

}

