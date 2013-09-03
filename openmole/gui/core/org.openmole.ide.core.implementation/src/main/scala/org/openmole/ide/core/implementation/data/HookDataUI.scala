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

import org.openmole.core.model.mole.IHook
import org.openmole.ide.misc.tools.util.ID
import scala.util.{ Try, Success }
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.panelsettings.HookPanelUI

abstract class HookDataUI extends DataUI
    with InputPrototype
    with OutputPrototype
    with ImplicitPrototype
    with ImageView
    with CoreObjectInitialisation
    with Clonable {

  type DATAUI = HookDataUI
  def implicitPrototypes: (List[PrototypeDataProxyUI], List[PrototypeDataProxyUI]) =
    coreObject match {
      case Success(x: IHook) ⇒ ToolDataUI.implicitPrototypes(y ⇒ x.inputs.map { _.prototype }.toList, inputs, y ⇒ x.outputs.map { _.prototype }.toList, outputs)
      case _                 ⇒ (List(), List())
    }

  def fatImagePath = ""

  override def toString: String = name

  def coreClass: Class[_ <: IHook]

  def coreObject: Try[IHook]

  def executionCoreObject = coreObject

  def buildPanelUI: HookPanelUI

  def doClone(p: PrototypeDataProxyUI): DATAUI = doClone(filterInputs(p), filterOutputs(p), filterInputParameters(p))
}