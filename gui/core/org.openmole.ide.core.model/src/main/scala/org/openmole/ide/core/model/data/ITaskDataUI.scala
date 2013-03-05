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

package org.openmole.ide.core.model.data

import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.core.model.workflow.{ IMoleScene, ICapsuleUI }
import org.openmole.core.model.mole.{ IMole, ICapsule }

trait ITaskDataUI extends IDataUI with InputPrototype with OutputPrototype with ImplicitPrototype {
  def name: String

  override def toString: String = name

  def coreObject(inputs: DataSet, outputs: DataSet, parameters: ParameterSet, plugins: PluginSet): ITask

  def filterPrototypeOccurencies(pproxy: IPrototypeDataProxyUI): List[IPrototypeDataProxyUI]

  def removePrototypeOccurencies(pproxy: IPrototypeDataProxyUI)

  def buildPanelUI: ITaskPanelUI

  def fatImagePath: String

  def imagePath: String = fatImagePath
}
