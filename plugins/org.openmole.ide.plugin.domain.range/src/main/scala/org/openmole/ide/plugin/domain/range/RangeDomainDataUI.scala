/*
 * Copyright (C) 2011 leclaire
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

package org.openmole.ide.plugin.domain.range

import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.domain.IDomain
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.plugin.domain.range._
import org.openmole.misc.tools.io.FromString._

class RangeDomainDataUI (
  val name: String="",
  val min: String = "",
  val max: String = "",
  val step: String = "") extends IDomainDataUI {
  
  override def coreObject(prototypeObject: IPrototype[_]): IDomain[_] = {
    if (prototypeObject.`type`.erasure == java.lang.Integer.TYPE) new Range[Int](min,max,step)
    else if (prototypeObject.`type`.erasure == java.lang.Double.TYPE) new Range[Double](min,max,step)
    else new Range[BigDecimal](min, max, step)
  }

  def coreClass = classOf[IDomain[_]]
  
  def buildPanelUI = new RangeDomainPanelUI(this)
  
}
