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

package org.openmole.ide.plugin.domain.collection

import java.math.BigDecimal
import java.math.BigInteger
import org.openmole.core.model.domain.Domain
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.data.{ IFactorDataUI, IDomainDataUI }
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.domain.collection.DynamicListDomain

object DynamicListDomainDataUI {

  def apply[T](values: List[String] = List(), classString: String) = {
    classString match {
      case "Int" ⇒ new DynamicListDomainDataUI[Int](values)
      case "Double" ⇒ new DynamicListDomainDataUI[Double](values)
      case "BigDecimal" ⇒ new DynamicListDomainDataUI[BigDecimal](values)
      case "BigInteger" ⇒ new DynamicListDomainDataUI[BigInteger](values)
      case "Long" ⇒ new DynamicListDomainDataUI[Long](values)
      case "String" ⇒ new DynamicListDomainDataUI[String](values)
      case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported")
    }
  }
}

class DynamicListDomainDataUI[T](val values: List[String])(implicit domainType: Manifest[T])
    extends IDomainDataUI[T] {

  val name = "Value list"

  def preview = " in " + values.headOption.getOrElse("") + " ..."

  override def coreObject(previousDomain: Option[IDomainDataUI[_]]): Domain[T] =
    new DynamicListDomain(values.toSeq: _*)

  def buildPanelUI = new DynamicListDomainPanelUI(this)

  def isAcceptable(domain: Option[IDomainDataUI[_]]) = false

  val availableTypes = List("Int", "Double", "BigDecimal", "BigInteger", "Long", "String")

  def coreClass = classOf[DynamicListDomainDataUI[T]]
}
