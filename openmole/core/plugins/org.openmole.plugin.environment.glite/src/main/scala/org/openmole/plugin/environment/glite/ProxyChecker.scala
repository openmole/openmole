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

package org.openmole.plugin.environment.glite

import org.openmole.misc.tools.service.Logger
import org.openmole.misc.updater.IUpdatableWithVariableDelay
import org.openmole.misc.workspace.Workspace
import scala.ref.WeakReference

object ProxyChecker extends Logger

import ProxyChecker._

class ProxyChecker(environment: WeakReference[GliteEnvironment]) extends IUpdatableWithVariableDelay {

  override def update: Boolean =
    environment.get match {
      case Some(env) ⇒
        try env.delegate
        catch {
          case (ex: Throwable) ⇒ logger.log(SEVERE, "Error while renewing the proxy", ex)
        }
        true
      case None ⇒ false
    }

  def delay = environment.get match {
    case Some(env) ⇒ GliteEnvironment.proxyRenewalDelay * 1000
    case None      ⇒ 0
  }
}
