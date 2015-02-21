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

import org.openmole.gui.ext.dataui.PanelUI
import rx._

package object dataui {

  def inoutputUI(protoDataBagUI: PrototypeDataBagUI, mappingsFactory: IOMappingsFactory = IOMappingsFactory.default) = {
    def mappings = new IOMappingsUI(mappingsFactory.build.fields.filter(_.prototypeFilter(protoDataBagUI)))
    val ioputUI = InOutputUI(protoDataBagUI.uuid, protoDataBagUI, Var(mappings))

    Obs(protoDataBagUI.dataUI) {
      ioputUI.mappings() = mappings
    }

    ioputUI
  }

  case class InOutputUI(id: String, protoDataBagUI: PrototypeDataBagUI, mappings: Var[IOMappingsUI])

}