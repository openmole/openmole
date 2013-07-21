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
package org.openmole.ide.core.model.data

import org.openmole.core.model.mole.ISource
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.panel.ISourcePanelUI
import scala.util.Try

trait ISourceDataUI extends IDataUI with InputPrototype with OutputPrototype with ImplicitPrototype {

  override def toString: String = name

  def coreClass: Class[_ <: ISource]

  def coreObject: Try[ISource]

  def executionCoreObject = coreObject

  def buildPanelUI: ISourcePanelUI

  def filterPrototypeOccurencies(pproxy: IPrototypeDataProxyUI) = (filterInputs(pproxy) ++ filterOutputs(pproxy)).distinct

  def cloneWithoutPrototype(proxy: IPrototypeDataProxyUI): ISourceDataUI = this

  def removePrototypeOccurencies(pproxy: IPrototypeDataProxyUI) = {
    removeInput(pproxy)
    removeOutput(pproxy)
  }
}