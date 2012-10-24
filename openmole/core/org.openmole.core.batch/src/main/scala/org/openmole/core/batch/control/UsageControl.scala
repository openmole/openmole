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

import org.openmole.misc.tools.service.Logger
import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import org.openmole.core.batch.environment.BatchService
import java.net.URI

import scala.concurrent.stm._

object UsageControl extends Logger {

  val unlimitedAccess = new UsageControl with UnlimitedAccess

  def apply(nbAccess: Int) = {
    if (nbAccess != Int.MaxValue)
      new UsageControl with LimitedAccess {
        val nbTokens = nbAccess
      }
    else unlimitedAccess
  }

  def withUsageControl[B](usageControl: UsageControl)(f: (AccessToken ⇒ B)): B = {
    val token = usageControl.waitAToken
    try f(token)
    finally usageControl.releaseToken(token)
  }

  def withToken[B](service: BatchService)(f: (AccessToken ⇒ B)): B = withUsageControl(get(service))(f)

  private val controls = new HashMap[String, UsageControl] with SynchronizedMap[String, UsageControl]

  def get(service: BatchService): UsageControl =
    controls.getOrElseUpdate(service.id, UsageControl(service.connections))
}

trait UsageControl {
  def waitAToken: AccessToken
  def tryGetToken: Option[AccessToken]
  def releaseToken(token: AccessToken)
  def available: Int
}
