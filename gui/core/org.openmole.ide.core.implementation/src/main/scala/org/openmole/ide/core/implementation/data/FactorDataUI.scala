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
import org.openmole.ide.core.model.sampling.{ ISamplingCompositionProxyUI, ISamplingProxyUI, IDomainProxyUI }
import org.openmole.core.model.sampling.Factor
import org.openmole.core.model.domain.Domain
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data.Prototype

case class FactorDataUI[T](val domain: IDomainProxyUI,
                           val target: Option[ISamplingCompositionProxyUI],
                           var prototype: Option[IPrototypeDataProxyUI] = None) extends IFactorDataUI {
  def coreObject = prototype match {
    case Some(p: IPrototypeDataProxyUI) ⇒ Factor(p.dataUI.coreObject.asInstanceOf[Prototype[T]],
      domain.dataUI.coreObject.asInstanceOf[Domain[T]])
    case _ ⇒ throw new UserBadDataError("A Prototype is required to link Domains and Samplings")
  }

  def clone(p: IPrototypeDataProxyUI) = copy(prototype = Some(p))

  def clone(d: IDomainProxyUI) = copy(domain = d)
}