package org.openmole.ide.core.implementation.panel

import org.openmole.ide.core.implementation.dataproxy.{ Proxies, PrototypeDataProxyUI }

/*
* Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

// Provides a IOProtoypePanel to deal with the prototype I/O

trait IO extends IOProxy {
  type DATAUI <: IODATAUI

  def ioPanel = {
    val (implicitIP, implicitOP) = proxy.dataUI.implicitPrototypes
    new IOPrototypePanel(
      Proxies.check(proxy.dataUI.inputs.toList),
      Proxies.check(proxy.dataUI.outputs.toList),
      implicitIP,
      implicitOP,
      proxy.dataUI.inputParameters.toMap)
  }

  def save(dUI: DATAUI,
           prototypesIn: Seq[PrototypeDataProxyUI],
           inputParameters: Map[PrototypeDataProxyUI, String],
           prototypesOut: Seq[PrototypeDataProxyUI]): DATAUI = {
    dUI.inputs = prototypesIn
    dUI.outputs = prototypesOut
    dUI.inputParameters = inputParameters
    dUI
  }

}
