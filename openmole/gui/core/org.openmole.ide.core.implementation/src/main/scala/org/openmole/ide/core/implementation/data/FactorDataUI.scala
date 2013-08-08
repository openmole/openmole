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

import org.openmole.core.model.sampling.Factor
import org.openmole.core.model.domain.Domain
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data.Prototype
import scala.util.Try
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.sampling.{ SamplingOrDomainProxyUI, DomainProxyUI }

case class FactorDataUI[T](val domain: DomainProxyUI,
                           val target: Option[SamplingOrDomainProxyUI],
                           var prototype: Option[PrototypeDataProxyUI] = None) extends IFactorDataUI {
  def coreObject =
    Try(prototype match {
      case Some(p: PrototypeDataProxyUI) ⇒
        Factor(p.dataUI.coreObject.get.asInstanceOf[Prototype[T]], domain.dataUI.coreObject.get.asInstanceOf[Domain[T]])
      case _ ⇒ throw new UserBadDataError("A Prototype is required to link Domains and Samplings")
    })

  def clone(p: PrototypeDataProxyUI) = copy(prototype = Some(p))

  def clone(d: DomainProxyUI) = copy(domain = d)
}