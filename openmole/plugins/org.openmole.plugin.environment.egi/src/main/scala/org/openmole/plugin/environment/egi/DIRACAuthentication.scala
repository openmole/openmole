/*
 * Copyright (C) 10/06/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.egi

import java.util.UUID

import fr.iscpif.gridscale.dirac.P12HTTPSAuthentication
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workspace.{ Workspace, AuthenticationProvider }

object DIRACAuthentication {

  def initialise(a: EGIAuthentication)(implicit authenticationProvider: AuthenticationProvider) =
    a match {
      case a: P12Certificate ⇒
        new P12HTTPSAuthentication {
          val certificate = a.certificate
          val password = a.password(authenticationProvider)
        }
      case _ ⇒ throw new UserBadDataError(s"Wrong authentication type ${a.getClass.getName} DIRAC only supports P12 authentication")
    }

}

