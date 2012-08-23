/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.data

import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import scala.collection.mutable.HashMap

abstract class TaskDataUI extends ITaskDataUI {
  var inputParameters: scala.collection.mutable.Map[IPrototypeDataProxyUI, String] = HashMap.empty[IPrototypeDataProxyUI, String]
  var prototypesIn = List.empty[IPrototypeDataProxyUI]
  var prototypesOut = List.empty[IPrototypeDataProxyUI]
  @transient var _implicitPrototypesIn: List[IPrototypeDataProxyUI] = _
  @transient var _implicitPrototypesOut: List[IPrototypeDataProxyUI] = _

  def implicitPrototypesIn = synchronized {
    if (_implicitPrototypesIn == null)
      _implicitPrototypesIn = List.empty
    _implicitPrototypesIn
  }

  def implicitPrototypesOut = synchronized {
    if (_implicitPrototypesOut == null)
      _implicitPrototypesOut = List.empty
    _implicitPrototypesOut
  }

  def updateImplicits(ipList: List[IPrototypeDataProxyUI],
                      opList: List[IPrototypeDataProxyUI]) = {
    _implicitPrototypesIn = ipList
    _implicitPrototypesOut = opList
  }

  def filterPrototypeOccurencies(pproxy: IPrototypeDataProxyUI) =
    (prototypesIn.filter(_ == pproxy) ++ prototypesOut.filter(_ == pproxy)).distinct

  def removePrototypeOccurencies(pproxy: IPrototypeDataProxyUI) = {
    prototypesIn = prototypesIn.filterNot { _ == pproxy }
    prototypesOut = prototypesOut.filterNot { _ == pproxy }
  }
}
