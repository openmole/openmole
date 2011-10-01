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

package org.openmole.plugin.environment.glite.authentication

import org.ogf.saga.context.Context

class PEMCertificate(cypheredPassword: String, certPath: String, keyPath: String) extends Certificate(cypheredPassword) {
  
  override protected def _init(ctx: Context) = {
    ctx.setAttribute(Context.USERCERT, certPath)
    ctx.setAttribute(Context.USERKEY, keyPath)
  }

}
