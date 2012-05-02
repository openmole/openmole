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

package org.openmole.plugin.environment.glite

import org.ogf.saga.context.Context
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.updater.IUpdatableWithVariableDelay
import org.openmole.misc.workspace.Workspace
import scala.ref.WeakReference

object ProxyChecker extends Logger {
  val defaultCheckTime = 30 * 1000 * 60
}

import ProxyChecker._

class ProxyChecker(
    context: Context,
    authentication: WeakReference[GliteAuthentication],
    expires: Option[Int] = None) extends IUpdatableWithVariableDelay {

  override def update: Boolean =
    authentication.get match {
      case Some(auth) ⇒
        try auth.reinit(context, expires)
        catch {
          case (ex: Throwable) ⇒ logger.log(SEVERE, "Error while renewing the proxy", ex)
        }
        true
      case None ⇒ false
    }

  def delay =
    try {
      val interval = (context.getAttribute(Context.LIFETIME).toLong * 1000 * Workspace.preferenceAsDouble(GliteEnvironment.ProxyRenewalRatio)).toLong
      logger.fine("Renew proxy in " + interval)
      interval
    } catch {
      case e ⇒
        logger.log(SEVERE, "Error while getting the check interval", e)
        defaultCheckTime
    }

  /*{
    val interval = lifeTime match {
      case Some(time) => time
      case None => context.getAttribute(Context.LIFETIME).toLong
    } 
    val renew = (interval * Workspace.preferenceAsDouble(GliteEnvironment.ProxyRenewalRatio) * 1000).toLong
    logger.fine("Renew proxy in " + renew)
    renew
  }*/

}
