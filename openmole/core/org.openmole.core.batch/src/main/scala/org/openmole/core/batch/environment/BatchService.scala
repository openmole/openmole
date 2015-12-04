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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.environment

import org.openmole.core.batch.control._

trait BatchService {
  def usageControl: UsageControl
  def environment: BatchEnvironment
  def tryGetToken: Option[AccessToken] = usageControl.tryGetToken
  def available: Int = usageControl.available
  def releaseToken(token: AccessToken): Unit = usageControl.releaseToken(token)
  def tryWithToken[B](f: (Option[AccessToken]) ⇒ B): B = usageControl.tryWithToken(f)
  def withToken[B](f: (AccessToken ⇒ B)): B = usageControl.withToken(f)
}
