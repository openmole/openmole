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

import fr.in2p3.jsaga.adaptor.security.VOMSContext
import org.ogf.saga.context.Context
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.updater.IUpdatableWithVariableDelay
import org.openmole.misc.workspace.Workspace
import scala.ref.WeakReference

object ProxyChecker extends Logger

import ProxyChecker._

class ProxyChecker(
    context: Context,
    authentication: WeakReference[GliteAuthentication],
    expires: Boolean) extends IUpdatableWithVariableDelay {

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
      val remainingTime =
        (if (context.getAttribute(Context.TYPE) == "VOMSMyProxy") context.getAttribute(VOMSContext.DELEGATIONLIFETIME).toLong
        else context.getAttribute(Context.LIFETIME).toLong) * 1000
      val interval =
        math.max(
          (remainingTime * Workspace.preferenceAsDouble(GliteEnvironment.ProxyRenewalRatio)).toLong,
          Workspace.preferenceAsDurationInMs(GliteEnvironment.MinProxyRenewal))

      logger.fine("Renew proxy in " + interval)
      interval
    } catch {
      case e ⇒
        logger.log(SEVERE, "Error while getting the check interval", e)
        Workspace.preferenceAsDurationInMs(GliteEnvironment.MinProxyRenewal)
    }

}
