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

import org.openmole.core.model.task.{ PluginSet, ITask }
import scala.util.{ Try, Failure, Success }
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.registry.PrototypeKey
import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxies }
import org.openmole.ide.core.implementation.commons.AggregationTransitionType
import org.openmole.ide.core.implementation.panel.{ TaskPanel, SaveSettings, Settings }
import org.openmole.ide.core.implementation.dialog.StatusBar

abstract class TaskDataUI extends DataUI
    with InputPrototype
    with OutputPrototype
    with ImplicitPrototype
    with ImageView
    with CoreObjectInitialisation
    with Clonable {

  type DATAUI = TaskDataUI

  override def toString: String = name

  def buildPanelUI: Settings with SaveSettings

  def coreObject(plugins: PluginSet): Try[ITask]

  def implicitPrototypes: (List[PrototypeDataProxyUI], List[PrototypeDataProxyUI]) = {
    coreObject(PluginSet.empty) match {
      case Success(x: ITask) ⇒
        ToolDataUI.implicitPrototypes(y ⇒ x.inputs.map { p ⇒
          p.prototype
        }.toList, inputs, y ⇒ x.outputs.map {
          _.prototype
        }.toList, outputs)
      case Failure(e) ⇒
        StatusBar().block("Fail in building " + name, e)
        (List(), List())
    }
  }

  def implicitPrototypesFromAggregation =
    ScenesManager().transitions.flatMap {
      t ⇒
        t.transitionType match {
          case AggregationTransitionType ⇒
            t.source.outputs
          case _ ⇒ List()
        }
    }.map {
      p ⇒
        Proxies.instance.prototypeOrElseCreate(PrototypeKey(p.dataUI.name, p.dataUI.`type`.runtimeClass, p.dataUI.dim + 1))
    }

  def doClone(p: PrototypeDataProxyUI): DATAUI = doClone(filterInputs(p), filterOutputs(p), filterInputParameters(p))

  def filterPrototypeOccurencies(pproxy: PrototypeDataProxyUI) = (filterInputs(pproxy) ++ filterOutputs(pproxy)).distinct
}