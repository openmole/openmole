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

package org.openmole.ide.plugin.domain.modifier

import java.math.BigDecimal
import java.math.BigInteger
import org.openmole.core.model.domain.{ Discrete, Domain }
import org.openmole.ide.misc.tools.util.Types._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.domain.modifier.GroupDomain
import org.openmole.ide.core.model.data.IDomainDataUI

object GroupDomainDataUI {

  def apply[S](size: String = "1",
               classString: String,
               previousDomain: Option[IDomainDataUI] = None) = {
    classString match {
      case INT ⇒ new GroupDomainDataUI[Int](size, previousDomain)
      case DOUBLE ⇒ new GroupDomainDataUI[Double](size, previousDomain)
      case BIG_DECIMAL ⇒ new GroupDomainDataUI[BigDecimal](size, previousDomain)
      case BIG_INTEGER ⇒ new GroupDomainDataUI[BigInteger](size, previousDomain)
      case LONG ⇒ new GroupDomainDataUI[Long](size, previousDomain)
      case STRING ⇒ new GroupDomainDataUI[String](size, previousDomain)
      case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported")
    }
  }
}

class GroupDomainDataUI[S](val size: String = "0",
                           var previousDomain: Option[IDomainDataUI] = None)(implicit val domainType: Manifest[S])
    extends ModifierDomainDataUI {

  val name = "Group"

  def preview = "Group (" + size + ")"

  override def coreObject: Domain[Any] = previousDomain match {
    case Some(pD: IDomainDataUI) ⇒ pD.coreObject match {
      case d: DOMAINTYPE ⇒ new GroupDomain(d, size.toInt)
      case _ ⇒ throw new UserBadDataError("No input domain has been found, it is required for a Group Domain.")
    }
    case _ ⇒ throw new UserBadDataError("No input domain has been found, it is required for a Group Domain.")
  }

  def buildPanelUI = new GroupDomainPanelUI(this)

  def coreClass = classOf[GroupDomainDataUI[S]]

  override def toString = "Group"

}
