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

import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.Event
import java.util.concurrent.TimeUnit
import org.openmole.misc.tools.service.Logger
import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap

import scala.concurrent.stm._

object UsageControl extends Logger {

  val botomlessUsage = new UsageControl(BotomlessTokenPool)

  def apply(nbAccess: Int) = {
    if (nbAccess != Int.MaxValue) new UsageControl(AccessTokenPool(nbAccess));
    else new UsageControl(BotomlessTokenPool)
  }

  def withUsageControl[B](usageControl: UsageControl, f: (AccessToken ⇒ B)): B = {
    val token = usageControl.waitAToken
    try f(token)
    finally usageControl.releaseToken(token)
  }

  def withToken[B](desc: ServiceDescription, f: (AccessToken ⇒ B)): B = {
    val usageControl = this.get(desc)
    withUsageControl(usageControl, f)
  }

  val controls = new HashMap[ServiceDescription, UsageControl] with SynchronizedMap[ServiceDescription, UsageControl]

  def register(ressource: ServiceDescription, usageControl: UsageControl) = {
    logger.fine("Register " + ressource)
    controls.getOrElseUpdate(ressource, usageControl)
  }

  def get(ressource: ServiceDescription) =
    controls.get(ressource) match {
      case Some(ctrl) ⇒ ctrl
      case None ⇒ botomlessUsage
    }

}

class UsageControl(tokenPool: IAccessTokenPool) {

  def waitAToken: AccessToken = tokenPool.waitAToken

  def tryGetToken: Option[AccessToken] = tokenPool.tryGetToken

  def releaseToken(token: AccessToken) = tokenPool.releaseToken(token)

}
