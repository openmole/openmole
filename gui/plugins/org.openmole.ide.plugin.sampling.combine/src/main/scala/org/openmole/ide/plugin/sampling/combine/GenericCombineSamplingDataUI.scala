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
package org.openmole.ide.plugin.sampling.combine

import org.openmole.ide.core.model.data.{ IDomainDataUI, ISamplingDataUI }
import org.openmole.core.model.domain.{ Discrete, Domain }
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.misc.exception.UserBadDataError

abstract class GenericCombineSamplingDataUI extends ISamplingDataUI {

  override def isAcceptable(domain: IDomainDataUI) =
    if (super.isAcceptable(domain)) true
    else {
      try {
        domain.coreObject match {
          case x: Domain[Any] with Discrete[Any] ⇒ true
          case _ ⇒
            StatusBar.warn("A Discrete Domain is required for a Complete Sampling")
            false
        }
      } catch {
        case u: UserBadDataError ⇒
          StatusBar.warn("This domain is not valid : " + u.getMessage)
          false
      }
    }
}