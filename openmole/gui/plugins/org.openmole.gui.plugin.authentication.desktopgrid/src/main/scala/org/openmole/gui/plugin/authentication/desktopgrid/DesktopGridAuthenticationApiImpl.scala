/**
 * Created by Mathieu Leclaire on 14/02/17.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
 *
 */
package org.openmole.gui.plugin.authentication.desktopgrid

import org.openmole.core.workspace._
import org.openmole.core.services._
import org.openmole.plugin.environment.desktopgrid.DesktopGridAuthentication

class DesktopGridAuthenticationApiImpl(s: Services) extends DesktopGridAuthenticationAPI {

  import s._

  def updateAuthentication(data: DesktopGridAuthenticationData): Unit = DesktopGridAuthentication.update(cypher.encrypt(data.password))

  def removeAuthentication(): Unit = DesktopGridAuthentication.clear

  def desktopGridAuthentications(): Seq[DesktopGridAuthenticationData] =
    DesktopGridAuthentication.passwordOption.map(DesktopGridAuthenticationData).toSeq

}