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

package org.openmole.core.batch.environment

import org.openmole.commons.tools.service.LockRepository
import scala.collection.mutable.HashMap

object AuthenticationRegistry {
  @transient private lazy val lockRepository = new LockRepository[BatchAuthenticationKey]
  private val registry = new HashMap[BatchAuthenticationKey, BatchAuthentication]
    
  def isRegistred(authenticationKey: BatchAuthenticationKey): Boolean = {
    registry.contains(authenticationKey)
  }

  def initAndRegisterIfNotAllreadyIs(key: BatchAuthenticationKey, authentication: BatchAuthentication) = synchronized {
    lockRepository.lock(key)
    try {
      if(!isRegistred(key)) {
        authentication.initialize
        registry.put(key, authentication)
      }
    } finally {
      lockRepository.unlock(key)
    }
  }
                                                                                    
  def registred (key: BatchAuthenticationKey): Option[BatchAuthentication] = synchronized {
    registry.get(key)
  }

}
