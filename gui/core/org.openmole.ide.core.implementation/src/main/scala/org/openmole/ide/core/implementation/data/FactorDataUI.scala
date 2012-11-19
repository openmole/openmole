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
package org.openmole.ide.core.implementation.data

import org.openmole.ide.core.model.data.IFactorDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.sampling.IDomainProxyUI
import org.openmole.core.model.sampling.Factor
import org.openmole.core.model.domain.Domain
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data.Prototype

object FactorDataUI {
  def empty = new FactorDataUI(None, None)
}

class FactorDataUI[T](val prototype: Option[IPrototypeDataProxyUI],
                      val domain: Option[IDomainProxyUI]) extends IFactorDataUI {
  def coreObject = prototype match {
    case Some(p: IPrototypeDataProxyUI) ⇒ domain match {
      case Some(d: IDomainProxyUI) ⇒ Factor(p.dataUI.coreObject.asInstanceOf[Prototype[T]],
        d.dataUI.coreObject.asInstanceOf[Domain[T]])
      case _ ⇒ throw new UserBadDataError("No Domain has been found to be linked with a Sampling")
    }
    case _ ⇒ throw new UserBadDataError("A Prototype is required to link Domains and Samplings")
  }
}