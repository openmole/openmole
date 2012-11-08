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
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.data.IFactorDataUI
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.domain.modifier.GroovyDomainModifier
import org.openmole.core.model.data.Prototype

object GroovyModifierDomainDataUI {

  def apply[T](code: String = "", classString: String) = {
    classString match {
      case "Int" ⇒ new GroovyModifierDomainDataUI[Int](code)
      case "Double" ⇒ new GroovyModifierDomainDataUI[Double](code)
      case "BigDecimal" ⇒ new GroovyModifierDomainDataUI[BigDecimal](code)
      case "BigInteger" ⇒ new GroovyModifierDomainDataUI[BigInteger](code)
      case "Long" ⇒ new GroovyModifierDomainDataUI[Long](code)
      case "String" ⇒ new GroovyModifierDomainDataUI[String](code)
      case x: Any ⇒ throw new UserBadDataError("The type " + x + " is not supported")
    }
  }
}

class GroovyModifierDomainDataUI[T](val code: String)(implicit domainType: Manifest[T])
    extends IDomainDataUI[T] {

  val name = "Map"

  def preview = " map( " + code.split("\n")(0) + " ...)"

  override def coreObject(prototype: IPrototypeDataProxyUI): Domain[T] = previousFactor match {
    case Some(f: IFactorDataUI) ⇒ f.domain match {
      case Some(d: Domain[_]) ⇒ new GroovyDomainModifier(prototype.dataUI.coreObject.asInstanceOf[Prototype[Any]],
        d.asInstanceOf[Domain[T] with Discrete[T]], code)
      case _ ⇒ throw new UserBadDataError("No input domain has been found, it is required for a Map Domain.")
    }
    case _ ⇒ throw new UserBadDataError("No input factor has been found, it is required for a Map Domain.")
  }

  def buildPanelUI(p: IPrototypeDataProxyUI) = new GroovyModifierDomainPanelUI(this, p)

  def buildPanelUI = buildPanelUI(new PrototypeDataProxyUI(GenericPrototypeDataUI[Double], false))

  def isAcceptable(p: IPrototypeDataProxyUI) = availableTypes.contains(p.dataUI.toString)

  val availableTypes = List("Int", "Double", "BigDecimal", "BigInteger", "Long", "String")

  def coreClass = classOf[GroovyModifierDomainDataUI[T]]
}
