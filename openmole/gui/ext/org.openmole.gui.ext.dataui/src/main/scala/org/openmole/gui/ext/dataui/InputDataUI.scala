package org.openmole.gui.ext.dataui

import org.openmole.gui.ext.data.{ InputData, OutputData, PrototypeData }
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

object InputDataUI {
  def build = new InputDataUI {
    def panelUI = new InputPanelUI(this)
  }
}

trait InputDataUI <: DataUI {
  type DATA = InputData

  def dataType = "Inputs"

  val inputsUI: InputsUI = Var(Seq())

  def data: DATA = new InputData {
    val inputs: Seq[(PrototypeData, Option[String])] = inputsUI().map { tu ⇒
      (tu()._1.data, tu()._2)
    }
  }
}