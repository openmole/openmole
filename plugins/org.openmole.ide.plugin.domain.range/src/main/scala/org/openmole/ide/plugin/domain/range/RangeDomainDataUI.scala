/*
 * Copyright (C) 2011 Mathieu Leclaire
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

import org.openmole.core.model.data.Prototype
import org.openmole.core.model.domain.IDomain
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.plugin.domain.range._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FromString._

class RangeDomainDataUI(
    val min: String = "0",
    val max: String = "",
    val step: String = "1") extends IDomainDataUI {

  override def coreObject(prototypeObject: Prototype[_]): IDomain[_] = {
    if (prototypeObject.`type`.erasure == java.lang.Integer.TYPE) new Range[Int](min, max, step)
    else if (prototypeObject.`type`.erasure == java.lang.Double.TYPE) new Range[Double](min, max, step)
    else if (prototypeObject.`type`.erasure == classOf[java.math.BigDecimal]) new Range[java.math.BigDecimal](min, max, step)
    else if (prototypeObject.`type`.erasure == classOf[java.math.BigInteger]) new Range[java.math.BigInteger](min, max, step)
    else throw new UserBadDataError("Unsupported range type " + prototypeObject.`type`.erasure)
  }

  def coreClass = classOf[IDomain[_]]

  def buildPanelUI = new RangeDomainPanelUI(this)

  override def toString = "Range"

  def preview = " on [" + min + "," + max + "," + step + "]"

  def isAcceptable(p: IPrototypeDataProxyUI) =
    //p.dataUI.coreObject.`type`.baseClasses.contains(typeOf[File].typeSymbol)
    p.dataUI.coreObject.`type`.erasure.isAssignableFrom(classOf[Double])
}
