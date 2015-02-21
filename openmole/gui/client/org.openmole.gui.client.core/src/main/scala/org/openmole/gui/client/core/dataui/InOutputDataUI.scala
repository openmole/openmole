package org.openmole.gui.client.core.dataui

import org.openmole.gui.client.core.GenericPanel
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.dataui.{ PanelUI, DataUI }
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

abstract class InOutputDataUI(mappingsFactory: IOMappingsFactory) extends DataUI {

  def dataType = "Inputs"

  def +=(proto: PrototypeDataBagUI) =
    if (!exists(proto)) {
      inoutputsUI() = inoutputsUI() :+ inoutputUI(proto, mappingsFactory)
    }

  def -=(inoutputUI: InOutputUI) = inoutputsUI() = inoutputsUI().filter {
    _.id != inoutputUI.id
  }

  def exists(inoutputUI: InOutputUI) = inoutputsUI().exists {
    _.id == inoutputUI.id
  }

  def exists(proto: PrototypeDataBagUI) = inoutputsUI().exists {
    _.protoDataBagUI.uuid == proto.uuid
  }

  val inoutputsUI: Var[Seq[InOutputUI]] = Var(Seq())

  def panelUI: PanelUI = PanelUI.empty

  def panelUI(panel: GenericPanel): InOutputPanelUI = new InOutputPanelUI(panel, this)

  def mappingKeys(p: PrototypeDataBagUI) = mappingsFactory.build.fields.filter { _.prototypeFilter(p) }.map { _.key }

}

class InputDataUI(mappingsFactory: IOMappingsFactory) extends InOutputDataUI((mappingsFactory)) {
  type DATA = InputData

  def data = new InputData {
    def inputs = inoutputsUI().map { id ⇒
      InOutput(id.protoDataBagUI.dataUI().data, id.mappings().fields.map { _.data })
    }
  }
}

class OutputDataUI(mappingsFactory: IOMappingsFactory) extends InOutputDataUI((mappingsFactory)) {
  type DATA = OutputData

  def data = new OutputData {
    def outputs = inoutputsUI().map { id ⇒
      InOutput(id.protoDataBagUI.dataUI().data, id.mappings().fields.map { _.data })
    }
  }
}

