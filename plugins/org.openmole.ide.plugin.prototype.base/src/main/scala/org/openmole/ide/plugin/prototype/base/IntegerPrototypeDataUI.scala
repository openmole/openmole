/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.plugin.prototype.base

import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.model.data.IPrototype

class IntegerPrototypeDataUI(val name: String = "", d: Int = 0) extends GenericPrototypeDataUI[Int](d) {
  def displayTypedName = toString + " : Int"

  def coreClass = classOf[IPrototype[Int]]

  def coreObject = new Prototype[Int](name).toArray(dim).asInstanceOf[IPrototype[Int]]

  def fatImagePath = "img/integer_fat.png"

  def buildPanelUI = new IntegerPrototypePanelUI(this)
}
