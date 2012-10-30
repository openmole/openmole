/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.model.commons

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.misc.exception.UserBadDataError

trait CapsuleType {
  def persistList: List[IPrototypeDataProxyUI] = List.empty
}

object CapsuleFactory {
  def apply(capsuleString: String) = {
    capsuleString match {
      case "BASIC" ⇒ new BasicCapsuleType
      case "MASTER" ⇒ new MasterCapsuleType
      case "STRAINER" ⇒ new StrainerCapsuleType
      case _ ⇒ throw new UserBadDataError("Unknown capsule type string " + capsuleString)
    }
  }
}

class MasterCapsuleType(override val persistList: List[IPrototypeDataProxyUI] = List.empty) extends CapsuleType {
  override def toString = "MASTER"
}

class BasicCapsuleType extends CapsuleType {
  override def toString = "BASIC"
}

class StrainerCapsuleType extends CapsuleType {
  override def toString = "STRAINER"
}