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

import org.openmole.core.model.domain.Domain
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.domain.modifier.GroovyDomainModifier
import org.openmole.ide.core.model.sampling.IFinite

case class GroovyModifierDomainDataUI(val prototypeName: String = "",
                                      val code: String = "",
                                      var previousDomain: List[IDomainDataUI] = List.empty)
    extends ModifierDomainDataUI with IFinite {

  def domainType = previousDomain.headOption match {
    case Some(d: IDomainDataUI) ⇒ d.domainType
    case _ ⇒ manifest[Double]
  }

  val name = "Map"

  def preview = "Map( " + code.split("\n").take(2).mkString(",") + " ...)"

  override def coreObject: Domain[Any] = {
    val valid = validPreviousDomains
    if (valid._1) new GroovyDomainModifier(valid._2.head, prototypeName, code)
    else throw new UserBadDataError("An input Domain is required for a Map modifier Domain")
  }

  def buildPanelUI(p: IPrototypeDataProxyUI) = new GroovyModifierDomainPanelUI(this)

  def buildPanelUI = buildPanelUI(new PrototypeDataProxyUI(GenericPrototypeDataUI[Double], false))

  def coreClass = classOf[GroovyModifierDomainDataUI]

  def clone(pD: List[IDomainDataUI]) = pD.headOption match {
    case Some(d: IDomainDataUI) ⇒ GroovyModifierDomainDataUI(prototypeName, code, pD)
    case _ ⇒ GroovyModifierDomainDataUI(prototypeName, code, List())
  }
}
