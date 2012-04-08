/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.domain.bounded

import org.openmole.core.model.domain.IBounded
import org.openmole.plugin.domain.bounded.DoubleBounded
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.model.data.IBoundedDomainDataUI

class BoundedDomainDataUI (val name: String="",
                                val min: String = "",
                                val max: String = "") extends IBoundedDomainDataUI {
  
  def coreObject(prototypeObject: IPrototype[Double]) = prototypeObject.`type` match {
    case x : Manifest[Double] => new DoubleBounded(min,max)
  }

  def coreClass = classOf[IBounded[Double]]
  
  def imagePath = "img/domain_range.png"
  
  def buildPanelUI = new BoundedDomainPanelUI(this)
  
}
