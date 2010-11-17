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

package org.openmole.core.batchservicecontrol.internal

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import org.openmole.commons.exception.InternalProcessingError
import  org.openmole.core.batchservicecontrol.AccessToken
import org.openmole.core.batchservicecontrol.IAccessTokenPool
import org.openmole.core.model.execution.batch.IAccessToken
import scala.collection.mutable.HashSet
import scala.collection.mutable.SynchronizedSet
import java.util.concurrent.TimeUnit

object AccessTokenPool {
  def apply(nbTokens: Int): AccessTokenPool = {
    val pool = new AccessTokenPool
    for (i <- 0 until nbTokens) {
      pool.add(new AccessToken)
    }
    pool
  }
}


class AccessTokenPool extends IAccessTokenPool {
  private val tokens = new LinkedBlockingDeque[IAccessToken]
  private val taken = new HashSet[IAccessToken] with SynchronizedSet[IAccessToken]
  private val _load = new AtomicInteger
  
  def add(token: IAccessToken) = {
    tokens.add(token)
    _load.decrementAndGet
  }

  override def waitAToken = {
    _load.incrementAndGet
    val token = try {
      tokens.take
    } catch {
      case (e) => 
        _load.decrementAndGet
        throw e
    }

    taken.add(token)
    token
  }

  override def waitAToken(time: Long, unit: TimeUnit): IAccessToken = {
    _load.incrementAndGet
    val ret = try {
      tokens.poll(time, unit)
    } catch {
      case (e) =>
        _load.decrementAndGet
        throw e
    }
    
    if (ret == null) {
      _load.decrementAndGet
      throw new TimeoutException
    }

    taken.add(ret)
    ret
  }

  override def releaseToken(token: IAccessToken) = {
    if (!taken.remove(token)) {
      throw new InternalProcessingError("Trying to release a token that hasn't been taken.")
    }

    tokens.add(token)
    _load.decrementAndGet
  }

  override def getAccessTokenInterruptly: IAccessToken = {
    _load.incrementAndGet
    val token = tokens.poll
    if (token != null) {
      taken.add(token)
    } else {
      _load.decrementAndGet
    }
    token
  }

  override def load: Int = {
    _load.get
  }
}
