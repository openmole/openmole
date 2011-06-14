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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.environment

import org.openmole.misc.tools.service.LockRepository
import scala.collection.mutable.HashMap

object AuthenticationRegistry {
  @transient private lazy val lockRepository = new LockRepository[String]
  private val registry = new HashMap[String, Authentication]
    
  def isRegistred(authenticationKey: String): Boolean = {
    registry.contains(authenticationKey)
  }

  def initAndRegisterIfNotAllreadyIs(authentication: Authentication) = synchronized {
    val key = authentication.key
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
                                                                                    
  def registred (key: String): Option[Authentication] = synchronized {
    registry.get(key)
  }

}
