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
import org.openmole.plugin.domain.bounded.Bounded
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.FromString._
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.core.model.domain.IDomain
import org.openmole.ide.core.model.data.IDomainDataUI

class BoundedDomainDataUI(val min: String = "",
                          val max: String = "") extends IDomainDataUI {

  def coreObject(prototypeObject: Prototype[_]) = prototypeObject.`type` match {
    case x: Manifest[Double] ⇒ new Bounded[Double](min, max)
    case _ ⇒ throw new UserBadDataError("The prototype " + prototypeObject + " has to be a Double on a bounded domain")
  }

  //Fix me with 2.10 reflexion
  // manifest[IDomain[Double] with IBounded[Double]]
  def coreClass = classOf[IDomain[Double]]

  def imagePath = "img/domain_range.png"

  def buildPanelUI = new BoundedDomainPanelUI(this)

  override def toString = "Bounded"

  def preview = " on [" + min + "," + max + "]"

  //FIXME : try to be changed in 2.10
  def isAcceptable(p: IPrototypeDataProxyUI) =
    p.dataUI.coreObject.`type`.erasure.isAssignableFrom(classOf[Double])

}
