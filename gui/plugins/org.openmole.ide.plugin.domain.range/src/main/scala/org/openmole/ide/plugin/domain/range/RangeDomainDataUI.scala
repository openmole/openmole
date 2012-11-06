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

import java.math.BigDecimal
import java.math.BigInteger
import org.openmole.core.model.data.Prototype
import org.openmole.core.model.domain.Domain
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.plugin.domain.range._
import org.openmole.plugin.domain.bounded._
import org.openmole.misc.tools.io.FromString
import org.openmole.misc.tools.io.FromString._
import org.openmole.misc.exception.UserBadDataError

object RangeDomainDataUI {

  def apply[T](min: String = "0", max: String = "", step: Option[String] = None, classString: String) = {
    classString match {
      case "Int" ⇒ new RangeDomainDataUI[Int](min, max, step)
      case "Double" ⇒ new RangeDomainDataUI[Double](min, max, step)
      case "BigDecimal" ⇒ new RangeDomainDataUI[BigDecimal](min, max, step)
      case "BigInteger" ⇒ new RangeDomainDataUI[BigInteger](min, max, step)
      case "Long" ⇒ new RangeDomainDataUI[Long](min, max, step)
      case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported")
    }
  }
}

class RangeDomainDataUI[T](
  val min: String = "0",
  val max: String = "",
  val step: Option[String] = None)(
    implicit domainType: Manifest[T],
    fs: FromString[T],
    integral: Integral[T])
    extends GenericRangeDomainDataUI[T] {

  val name = "Range"

  override def coreObject(prototype: IPrototypeDataProxyUI,
                          domain: Option[IDomainDataUI[_]]): Domain[T] = step match {
    case Some(s: String) ⇒
      if (s.isEmpty) new Bounded[T](min, max)
      else new Range[T](min, max, stepString)
    case _ ⇒ new Bounded[T](min, max)
  }

  def buildPanelUI(p: IPrototypeDataProxyUI) = new RangeDomainPanelUI(this, p)

  val availableTypes = List("Int", "Double", "BigDecimal", "BigInteger", "Long")

  def coreClass = classOf[RangeDomainDataUI[T]]

  override def toString = "Range"

}
