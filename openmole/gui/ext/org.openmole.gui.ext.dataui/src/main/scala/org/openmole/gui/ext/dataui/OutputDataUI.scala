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

import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.ProtoTYPE._
import rx._

class OutputDataUI(val ioMapping: Boolean, val prototypeFilter: Seq[ProtoTYPE] = ALL) extends DataUI {
  type DATA = OutputData

  def dataType = "Outputs"

  val outputsUI: Var[Seq[OutputUI]] = Var(Seq())

  def panelUI = new OutputPanelUI(this)

  def data = new OutputData {
    def outputs = outputsUI().map { o ⇒
      Output(o.protoDataBagUI.dataUI().data, o.mapping())
    }
  }
}
