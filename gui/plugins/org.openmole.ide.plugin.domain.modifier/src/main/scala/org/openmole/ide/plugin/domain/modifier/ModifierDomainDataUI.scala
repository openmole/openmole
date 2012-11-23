/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

import org.openmole.core.model.domain.{ Discrete, Domain }
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.model.sampling.IModifier

trait ModifierDomainDataUI extends IDomainDataUI with IModifier {
  type DOMAINTYPE = Domain[Any] with Discrete[Any]

  override def isAcceptable(domain: IDomainDataUI): Boolean =
    domain.coreObject match {
      case d: DOMAINTYPE ⇒ domain.domainType == domainType
      case _ ⇒
        StatusBar.warn("A Discrete Domain is required as input of a Modifier Domain (Map, Take, Group, ...)")
        false
    }

  def validPreviousDomains: (Boolean, List[DOMAINTYPE]) = {
    val dL = previousDomain.flatMap {
      _.coreObject match {
        case id: DOMAINTYPE ⇒ List(id)
        case _ ⇒ Nil
      }
    }
    (!dL.isEmpty, dL)
  }
}