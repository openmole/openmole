/*
 * Copyright (C) 2010 reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.authenticationregistry.internal

import org.openmole.core.authenticationregistry.IAuthenticationRegistry
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey
import scala.collection.mutable.HashMap

class AuthenticationRegistry extends IAuthenticationRegistry {

  private val registry = new HashMap[IBatchServiceAuthenticationKey[_], IBatchServiceAuthentication]
    
  override def isRegistred(authenticationKey: IBatchServiceAuthenticationKey[_]): Boolean = synchronized {
    registry.contains(authenticationKey)
  }

  override def initAndRegisterIfNotAllreadyIs[AUTH <: IBatchServiceAuthentication](key: IBatchServiceAuthenticationKey[AUTH], authentication: AUTH) = synchronized {
    if(!isRegistred(key)) {
      authentication.initialize
      registry.put(key, authentication)
    }
  }
                                                                                    
  override def registred[AUTH <: IBatchServiceAuthentication] (key: IBatchServiceAuthenticationKey[AUTH]): Option[AUTH] = synchronized {
    registry.get(key).asInstanceOf[Option[AUTH]]
  }

}
