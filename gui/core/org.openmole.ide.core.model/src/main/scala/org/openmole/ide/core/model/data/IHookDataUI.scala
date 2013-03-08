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

import org.openmole.core.model.mole._
import org.openmole.ide.core.model.dataproxy.{ IPrototypeDataProxyUI, ITaskDataProxyUI }
import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.core.model.data.Prototype
import org.openmole.ide.misc.tools.util.ID

trait IHookDataUI extends IDataUI with InputPrototype with OutputPrototype with ImplicitPrototype {
  def id: ID.Type

  override def toString: String = name

  def coreClass: Class[_ <: IHook]

  def coreObject(protoMapping: Map[IPrototypeDataProxyUI, Prototype[_]]): List[IHook]

  def buildPanelUI: IHookPanelUI
}
