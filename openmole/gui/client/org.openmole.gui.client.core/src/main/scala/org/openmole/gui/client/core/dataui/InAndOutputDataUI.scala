package org.openmole.gui.client.core.dataui

/*
 * Copyright (C) 26/02/15 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.core.GenericPanel
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.dataui.{ PanelUI, DataUI }
import rx._

class InAndOutputDataUI(val mappingsFactory: IOMappingsFactory) extends DataUI {

  val inputDataUI = new InputDataUI(IOMappingsFactory.default)
  val outputDataUI = new OutputDataUI(IOMappingsFactory.empty)
  val inAndOutputsUI: Var[Seq[InAndOutputUI]] = Var(Seq())

  type DATA = InAndOutputData

  def dataType = "Inputs and Outputs"

  def +=(in: PrototypeDataBagUI, out: PrototypeDataBagUI, mapping: IOMappingDataUI[_]) = {
    val iAo = inAndOutputUI(in, out, mapping)
    if (!exists(iAo)) {
      inAndOutputsUI() = inAndOutputsUI() :+ iAo
    }
  }

  def -=(inoutputUI: InAndOutputUI) = inAndOutputsUI() = inAndOutputsUI().filter {
    _.id != inoutputUI.id
  }

  def exists(iAo: InAndOutputUI) = inAndOutputsUI().exists(_ == iAo)

  def data = new InAndOutputData {

    def inAndOutputs = inAndOutputsUI().map { i ⇒
      InAndOutput(
        i.in.protoDataBagUI.dataUI().data,
        i.out.protoDataBagUI.dataUI().data,
        (new IOMappingsUI(mappingsFactory.build.fields)).fields.head.data)
    }

    def inputs = InOutputUI.inputData(inputDataUI.inoutputsUI())

    def outputs = InOutputUI.outputData(outputDataUI.inoutputsUI())
  }

  def panelUI: PanelUI = PanelUI.empty

  def panelUI(panel: GenericPanel): InAndOutPanelUI = new InAndOutPanelUI(panel, this)
}
