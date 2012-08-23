/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.environment.glite

import org.ogf.saga.context.Context
import GliteAuthentication._
import org.openmole.core.batch.authentication.JSAGASessionService
import fr.in2p3.jsaga.adaptor.security.VOMSContext

class VOMSProxyFile(val proxyFile: String) extends GliteAuthenticationMethod {

  def init(authentication: GliteAuthentication): (Context, Boolean) = {
    val ctx = JSAGASessionService.createContext

    ctx.setAttribute(Context.TYPE, "VOMS")
    ctx.setAttribute(Context.USERPROXY, proxyFile)
    ctx.setAttribute(Context.CERTREPOSITORY, CACertificatesDir.getCanonicalPath)
    ctx.setAttribute(VOMSContext.VOMSDIR, "")
    ctx.setAttribute(VOMSContext.PROXYTYPE, "RFC820")
    (ctx, true)
  }

  override def toString = "VOMS proxy file path = " + proxyFile
}
