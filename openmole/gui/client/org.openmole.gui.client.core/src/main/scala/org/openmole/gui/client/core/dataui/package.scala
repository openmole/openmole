package org.openmole.gui.client.core

/*
 * Copyright (C) 16/12/14 // mathieu.leclaire@openmole.org
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
import rx._

package object dataui {

  def inoutputUI(protoDataBagUI: PrototypeDataBagUI, mappingsFactory: IOMappingsFactory) = {
    //def mappings = new IOMappingsUI(mappingsFactory.build.fields.filter(_.prototypeFilter(protoDataBagUI)))
    val ioputUI = InOutputUI(protoDataBagUI.uuid, protoDataBagUI, Var(new IOMappingsUI(mappingsFactory.build.fields.filter(_.prototypeFilter(protoDataBagUI)))))

    /* Obs(protoDataBagUI.dataUI) {
      ioputUI.mappings() = mappings
    }*/
    ioputUI
  }

  def defaultInOutputUI(protoDataBagUI: PrototypeDataBagUI) = inoutputUI(protoDataBagUI, IOMappingsFactory.default)

  def emptyInOutputUI(protoDataBagUI: PrototypeDataBagUI) = inoutputUI(protoDataBagUI, IOMappingsFactory.empty)

  def inAndOutputUI(i: PrototypeDataBagUI, o: PrototypeDataBagUI, mapping: IOMappingDataUI[_]) = {
    val in = inoutputUI(i, IOMappingsFactory.default)
    val out = inoutputUI(o, IOMappingsFactory.empty)
    InAndOutputUI(i.uuid + o.uuid, in, out, mapping)
  }

  object InOutputUI {
    def inputData(inoutputsUI: Seq[InOutputUI]) = inoutputsUI.map { id ⇒
      InOutput(id.protoDataBagUI.dataUI().data, id.mappings().fields.map { _.data })
    }

    def outputData(inoutputsUI: Seq[InOutputUI]) = inoutputsUI.map { id ⇒
      InOutput(id.protoDataBagUI.dataUI().data, id.mappings().fields.map { _.data })
    }
  }

  case class InOutputUI(id: String, protoDataBagUI: PrototypeDataBagUI, mappings: Var[IOMappingsUI])

  case class InAndOutputUI(id: String, in: InOutputUI, out: InOutputUI, mapping: IOMappingDataUI[_])

}