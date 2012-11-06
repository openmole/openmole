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
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.domain.modifier.TakeDomain
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI

object TakeDomainDataUI {

  def apply[T](size: String = "1",
               classString: String) =
    classString match {
      case "Int" ⇒ new TakeDomainDataUI[Int](size)
      case "Double" ⇒ new TakeDomainDataUI[Double](size)
      case "BigDecimal" ⇒ new TakeDomainDataUI[BigDecimal](size)
      case "BigInteger" ⇒ new TakeDomainDataUI[BigInteger](size)
      case "Long" ⇒ new TakeDomainDataUI[Long](size)
      case "String" ⇒ new TakeDomainDataUI[String](size)
      case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported")
    }
}

class TakeDomainDataUI[T](val size: String = "0")(implicit domainType: Manifest[T])
    extends IDomainDataUI[T] {

  val name = "Take"

  def preview = " take " + size

  def isAcceptable(protoProxy: IPrototypeDataProxyUI) = availableTypes.contains(protoProxy.dataUI.toString)

  override def coreObject(prototype: IPrototypeDataProxyUI,
                          domain: Option[Domain[_]]): Domain[T] = domain match {
    case Some(d: Domain[T] with Discrete[T]) ⇒ new TakeDomain[T](d, size.toInt)
    case _ ⇒ throw new UserBadDataError("No input domain has been found, it is required for a Take Domain.")
  }

  def buildPanelUI = buildPanelUI(new PrototypeDataProxyUI(GenericPrototypeDataUI[Int], false))

  def buildPanelUI(p: IPrototypeDataProxyUI) = new TakeDomainPanelUI(this, p)

  val availableTypes = List("Int", "Double", "BigDecimal", "BigInteger", "Long", "String")

  def coreClass = classOf[TakeDomainDataUI[T]]

  override def toString = "Take"

}
