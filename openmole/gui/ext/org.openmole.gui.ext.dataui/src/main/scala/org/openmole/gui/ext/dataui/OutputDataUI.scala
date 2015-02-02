package org.openmole.gui.ext.dataui

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

import org.openmole.gui.ext.data.{ OutputData, PrototypeData }
import rx._

object OutputDataUI {
  def build = new OutputDataUI {
    def panelUI = new OutputPanelUI(this)
  }
}

trait OutputDataUI <: DataUI {
  type DATA = OutputData

  def dataType = "Outputs"

  val outputsUI: OutputsUI = Var(Seq())

  def data: DATA = new OutputData {
    val outputs: Seq[PrototypeData] = outputsUI().map {
      _().data
    }
  }
}
