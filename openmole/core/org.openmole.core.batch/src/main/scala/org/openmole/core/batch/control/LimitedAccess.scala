/*
 * Copyright (C) 2010 Romain Reuillon
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

import scala.concurrent.duration._
import scala.concurrent.stm._

class LimitedAccess(val nbTokens: Int, val maxByPeriod: Int) extends UsageControl { la ⇒

  class LimitedAccessToken extends AccessToken {
    override def access[T](op: ⇒ T): T = synchronized {
      la.accessed()
      op
    }
  }

  def period = 1 minute

  private lazy val tokens: Ref[List[AccessToken]] = Ref((0 until nbTokens).map { i ⇒ new LimitedAccessToken }.toList)
  private lazy val taken: TSet[AccessToken] = TSet()
  private lazy val accesses: Ref[List[Long]] = Ref(List())

  private def add(token: AccessToken) = atomic { implicit txn ⇒
    tokens() = token :: tokens()
    taken -= token
  }

  def releaseToken(token: AccessToken) = add(token)

  private def checkAccessRate() = atomic { implicit txn ⇒
    clearOldAccesses()
    if (accesses().size < maxByPeriod) true
    else false
  }

  private def accessed() = atomic { implicit txn ⇒
    clearOldAccesses()
    accesses() = System.currentTimeMillis() :: accesses()
  }

  private def clearOldAccesses() = atomic { implicit txn ⇒
    val now = System.currentTimeMillis()
    accesses() = accesses().takeWhile(_ > now - period.toMillis)
  }

  def tryGetToken: Option[AccessToken] = atomic { implicit txn ⇒
    def allReadyHasAToken = taken.find(Thread.holdsLock)
    def tryGet = (checkAccessRate(), tokens()) match {
      case (true, head :: tail) ⇒
        taken += head
        tokens() = tail
        Some(head)
      case _ ⇒ None
    }
    allReadyHasAToken orElse tryGet
  }

  def waitAToken: AccessToken = atomic { implicit txn ⇒
    tryGetToken.getOrElse(retry)
  }

  def available = tokens.single().size
}
