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

package org.openmole.core.batch.control

import org.openmole.commons.aspect.eventdispatcher.ObjectModified

import java.util.concurrent.TimeUnit

object UsageControl {
  
  final val ResourceReleased = "ResourceReleased"
  
  def apply(nbAccess: Int) = {
    if (nbAccess != Int.MaxValue) {
      new UsageControl(AccessTokenPool(nbAccess));
    } else {
      new UsageControl(BotomlessTokenPool)
    }
  }

  def withToken[B]( desc: BatchServiceDescription, f: (AccessToken => B)): B = {
    val usageControl = BatchServiceControl.usageControl(desc)
    val token = usageControl.waitAToken
    try {
      f(token)
    } finally {
      usageControl.releaseToken(token)
    }
  }

}


class UsageControl(tokenPool: IAccessTokenPool) {

  def waitAToken(time: Long, unit: TimeUnit): AccessToken = tokenPool.waitAToken(time, unit)

  def waitAToken: AccessToken = tokenPool.waitAToken

  def tryGetToken: Option[AccessToken] = tokenPool.tryGetToken
  
  @ObjectModified(name = UsageControl.ResourceReleased)
  def releaseToken(token: AccessToken) = tokenPool.releaseToken(token)
 
  def load: Int = tokenPool.load
  
}
