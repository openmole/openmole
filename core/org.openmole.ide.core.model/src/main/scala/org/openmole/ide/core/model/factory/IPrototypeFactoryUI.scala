/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.model.factory

import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.model.data.IPrototypeDataUI

trait IPrototypeFactoryUI[T] extends IFactoryUI with IMoleComponent {
  override def displayName: String = buildDataUI.coreClass.getSimpleName

  def buildDataUI: IPrototypeDataUI[T]

  def buildDataUI(prototype: IPrototype[_]): IPrototypeDataUI[T]
}