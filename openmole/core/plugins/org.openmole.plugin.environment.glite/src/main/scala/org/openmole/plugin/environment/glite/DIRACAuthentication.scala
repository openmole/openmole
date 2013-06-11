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

package org.openmole.plugin.environment.glite

import fr.iscpif.gridscale.authentication.P12HTTPSAuthentication
import org.openmole.misc.workspace.{ ConfigurationLocation, Workspace }

object DIRACAuthentication {
  def update(a: DIRACAuthentication) = Workspace.persistentList(classOf[DIRACAuthentication])(0) = a
  def apply() = Workspace.persistentList(classOf[DIRACAuthentication])(0)
  def get = Workspace.persistentList(classOf[DIRACAuthentication]).get(0)

  def initialise(a: DIRACAuthentication) =
    a match {
      case a: P12Certificate ⇒
        new P12HTTPSAuthentication {
          val certificate = a.certificate
          val password = a.password
        }
    }

}

trait DIRACAuthentication extends GliteAuthentication

