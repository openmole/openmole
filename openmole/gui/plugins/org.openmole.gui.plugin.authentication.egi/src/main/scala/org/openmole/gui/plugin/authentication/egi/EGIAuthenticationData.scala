package org.openmole.gui.plugin.authentication.egi

import org.openmole.gui.shared.data.{AuthenticationData, ErrorData, MessageErrorData, SafePath, Test}
import org.openmole.gui.shared.data

/*
 * Copyright (C) 12/01/17 // mathieu.leclaire@openmole.org
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

case class EGIAuthenticationData(
  password:         String         = "",
  privateKey:       Option[SafePath] = None
) extends AuthenticationData {
  def name = "egi.p12"
}
