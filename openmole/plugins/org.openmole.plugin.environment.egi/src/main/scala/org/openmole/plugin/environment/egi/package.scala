/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.environment

import java.net.URI

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.exception._
import org.openmole.core.serializer.SerializerService
import org.openmole.core.workspace.Workspace

package object egi {
  //  implicit def egiAuthentication(implicit workspace: Workspace, authenticationStore: AuthenticationStore, serializerService: SerializerService): EGIAuthentication = EGIAuthentication().getOrElse(throw new UserBadDataError("No authentication was found"))

  implicit def stringToBDII(s: String) = {
    val uri = new URI(s)
    _root_.gridscale.egi.BDIIServer(uri.getHost, uri.getPort)
  }

}