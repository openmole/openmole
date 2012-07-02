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

package org.openmole.core.batch.control

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import org.openmole.misc.exception.InternalProcessingError
import scala.collection.mutable.HashSet
import scala.collection.mutable.SynchronizedSet
import java.util.concurrent.TimeUnit

object AccessTokenPool {
  def apply(nbTokens: Int): AccessTokenPool = {
    val pool = new AccessTokenPool
    for (i ← 0 until nbTokens) pool.add(new AccessToken)
    pool
  }
}

class AccessTokenPool extends IAccessTokenPool {
  private val tokens = new LinkedBlockingDeque[AccessToken]
  private val taken = new HashSet[AccessToken] with SynchronizedSet[AccessToken]
  private val _load = new AtomicInteger

  def add(token: AccessToken) = {
    tokens.add(token)
    _load.decrementAndGet
  }

  override def waitAToken = {
    _load.incrementAndGet
    val token = 
      try tokens.take
      catch {
        case e ⇒
          _load.decrementAndGet
          throw e
      }

    taken.add(token)
    token
  }

  override def waitAToken(time: Long, unit: TimeUnit): AccessToken = {
    _load.incrementAndGet
    val ret = try {
      tokens.poll(time, unit)
    } catch {
      case (e) ⇒
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

  override def releaseToken(token: AccessToken) = {

    if (!taken.remove(token)) {
      throw new InternalProcessingError("Trying to release a token that hasn't been taken.")
    }

    tokens.add(token)
    _load.decrementAndGet
  }

  override def tryGetToken: Option[AccessToken] = {
    _load.incrementAndGet
    tokens.poll match {
      case null ⇒
        _load.decrementAndGet
        return None
      case token ⇒
        taken.add(token)
        return Some(token)
    }
  }

  override def load: Int = _load.get

}
