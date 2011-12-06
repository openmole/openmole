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

import java.util.logging.Level
import java.util.logging.Logger
import org.ogf.saga.context.Context
import org.openmole.misc.updater.IUpdatable
import scala.ref.WeakReference

class ProxyChecker(context: Context, duration: Option[Int], authentication: WeakReference[GliteAuthentication]) extends IUpdatable {

  override def update: Boolean =
    authentication.get match {
      case Some(auth) => 
        try auth.reinit(context, duration)
        catch {
          case(ex: Throwable) => Logger.getLogger(classOf[ProxyChecker].getName).log(Level.SEVERE, "Error while renewing the proxy.", ex);
        } 
        true
      case None =>
        false
    }
  
}
