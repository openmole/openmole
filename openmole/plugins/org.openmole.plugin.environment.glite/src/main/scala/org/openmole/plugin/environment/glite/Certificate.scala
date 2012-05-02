/*
 * Copyright (C) 2011 reuillon
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
import org.openmole.misc.workspace.Workspace
import fr.in2p3.jsaga.adaptor.security.VOMSContext
import org.openmole.core.batch.jsaga.JSAGASessionService
import GliteAuthentication._

import GliteAuthentication._

abstract class Certificate(cypheredPassword: String) extends GliteAuthenticationMethod {

  //@transient lazy val proxy = Workspace.newFile("proxy", ".x509")

  def password =
    if (cypheredPassword == null) ""
    else Workspace.decrypt(cypheredPassword)

  override def init(authentication: GliteAuthentication) = {
    import authentication._

    val ctx = JSAGASessionService.createContext
    ctx.setAttribute(VOMSContext.VOMSDIR, "")

    ctx.setAttribute(Context.CERTREPOSITORY, CACertificatesDir.getCanonicalPath)

    val proxyDuration = myProxy match {
      case Some(proxy) ⇒
        ctx.setAttribute(Context.TYPE, "VOMSMyProxy")
        ctx.setAttribute(VOMSContext.MYPROXYSERVER, proxy.url)
        ctx.setAttribute(VOMSContext.MYPROXYUSERID, proxy.userId)
        ctx.setAttribute(VOMSContext.MYPROXYPASS, proxy.pass)
        //ctx.setAttribute(VOMSContext.DELEGATIONLIFETIME, getTimeString)
        None
      case None ⇒
        ctx.setAttribute(Context.TYPE, "VOMS")
        Some(inMs(getTimeString))
    }

    ctx.setAttribute(Context.LIFETIME, getTimeString)
    ctx.setAttribute(Context.USERPASS, password)

    if (!fqan.isEmpty) ctx.setAttribute(VOMSContext.USERFQAN, fqan)

    val proxyFile = Workspace.newFile("proxy", ".x509")
    ctx.setAttribute(Context.USERPROXY, proxyFile.getAbsolutePath)

    ctx.setAttribute(Context.SERVER, vomsURL)
    ctx.setAttribute(Context.USERVO, voName)

    _init(ctx)

    (ctx, proxyDuration)
  }

  protected def _init(ctx: Context)

}
