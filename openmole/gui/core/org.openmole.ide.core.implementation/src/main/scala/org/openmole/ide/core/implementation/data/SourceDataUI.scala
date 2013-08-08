/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.data

import org.openmole.core.model.mole.ISource
import org.openmole.ide.misc.tools.util.ID
import scala.util.{ Success, Try }
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.panelsettings.SourcePanelUI

abstract class SourceDataUI extends DataUI
    with InputPrototype
    with OutputPrototype
    with ImplicitPrototype
    with ImageView
    with CoreObjectInitialisation
    with ID {

  override def toString: String = name

  def coreClass: Class[_ <: ISource]

  def coreObject: Try[ISource]

  def executionCoreObject = coreObject

  def buildPanelUI: SourcePanelUI

  def filterPrototypeOccurencies(pproxy: PrototypeDataProxyUI) = (filterInputs(pproxy) ++ filterOutputs(pproxy)).distinct

  def cloneWithoutPrototype(proxy: PrototypeDataProxyUI): SourceDataUI = this

  def removePrototypeOccurencies(pproxy: PrototypeDataProxyUI) = {
    removeInput(pproxy)
    removeOutput(pproxy)
  }

  def implicitPrototypes: (List[PrototypeDataProxyUI], List[PrototypeDataProxyUI]) =
    coreObject match {
      case Success(x) ⇒ ToolDataUI.implicitPrototypes(y ⇒ x.inputs.toList.map { _.prototype }, inputs, y ⇒ x.outputs.toList.map { _.prototype }, outputs)
      case _          ⇒ (List(), List())
    }

  override def fatImagePath = "img/source_fat.png"

  override def imagePath = "img/source.png"
}
