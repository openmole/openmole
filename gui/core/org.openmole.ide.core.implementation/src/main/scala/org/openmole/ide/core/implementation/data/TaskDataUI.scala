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

import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import scala.collection.mutable.HashMap
import org.openmole.ide.core.implementation.registry.KeyPrototypeGenerator
import org.openmole.ide.core.implementation.builder.MoleFactory
import org.openmole.core.model.task.ITask

abstract class TaskDataUI extends ITaskDataUI {
  var inputParameters: scala.collection.mutable.Map[IPrototypeDataProxyUI, String] = HashMap.empty[IPrototypeDataProxyUI, String]

  var inputs = List.empty[IPrototypeDataProxyUI]

  var outputs = List.empty[IPrototypeDataProxyUI]

  def implicitPrototypes: (List[IPrototypeDataProxyUI], List[IPrototypeDataProxyUI]) =
    MoleFactory.taskCoreObject(this) match {
      case Right(x: ITask) ⇒ ToolDataUI.implicitPrototypes(y ⇒ x.inputs, inputs, y ⇒ x.outputs, outputs)
      case _ ⇒ (List(), List())
    }

  def removePrototypeOccurencies(pproxy: IPrototypeDataProxyUI) = {
    inputs = inputs.filterNot { _ == pproxy }
    outputs = outputs.filterNot { _ == pproxy }
  }

  def filterPrototypeOccurencies(pproxy: IPrototypeDataProxyUI) =
    (inputs.filter(_ == pproxy) ++ outputs.filter(_ == pproxy)).distinct
}