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

package org.openmole.core.batchservicecontrol

import org.openmole.commons.aspect.eventdispatcher.ObjectModified

import org.openmole.core.batchservicecontrol.internal.AccessTokenPool
import org.openmole.core.batchservicecontrol.internal.BotomlessTokenPool
import org.openmole.core.model.execution.batch.IAccessToken
import java.util.concurrent.TimeUnit

object UsageControl {
  def apply(nbAccess: Int) = {
    if (nbAccess != Int.MaxValue) {
      new UsageControl(AccessTokenPool(nbAccess));
    } else {
      new UsageControl(BotomlessTokenPool)
    }
  }
}


class UsageControl(tokenPool: IAccessTokenPool) extends IUsageControl {

  override def waitAToken(time: Long, unit: TimeUnit): IAccessToken = tokenPool.waitAToken(time, unit)

  override def waitAToken: IAccessToken = tokenPool.waitAToken

  override def tryGetToken: Option[IAccessToken] = tokenPool.tryGetToken
  
  @ObjectModified(name = IUsageControl.ResourceReleased)
  override def releaseToken(token: IAccessToken) = tokenPool.releaseToken(token)
 
  override def load: Int = tokenPool.load
  
}
