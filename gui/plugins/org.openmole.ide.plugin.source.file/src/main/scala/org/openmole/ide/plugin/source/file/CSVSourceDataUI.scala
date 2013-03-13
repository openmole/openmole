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

package org.openmole.ide.plugin.source.file

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.implementation.data.SourceDataUI
import org.openmole.plugin.source.file.CSVSource
import java.io.File
import org.openmole.ide.core.implementation.data.EmptyDataUIs.EmptyPrototypeDataUI

class CSVSourceDataUI(val name: String = "",
                      val csvFilePath: String = "",
                      val prototypeMapping: List[(String, IPrototypeDataProxyUI)] = List.empty) extends SourceDataUI {

  def coreClass = classOf[CSVSource]

  def buildPanelUI = new CSVSourcePanelUI(this)

  def coreObject(protoMapping: Map[IPrototypeDataProxyUI, Prototype[_]]) = {
    val source = CSVSource(name, new File(csvFilePath))
    prototypeMapping.filter(!_._2.dataUI.isInstanceOf[EmptyPrototypeDataUI]).foreach {
      m ⇒ source addColumn (m._1, m._2.dataUI.coreObject)
    }
    source
  }
}
