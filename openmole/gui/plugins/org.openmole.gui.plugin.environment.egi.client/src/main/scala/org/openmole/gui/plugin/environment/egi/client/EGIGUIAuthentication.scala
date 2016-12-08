/**
 * Created by Romain Reuillon on 28/11/16.
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
package org.openmole.gui.plugin.environment.egi.client

import org.openmole.gui.ext.data.Authentication

import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport
class EGIGUIAuthentication extends Authentication {

  @JSExport
  def test: Unit = Utils.uuid

  @JSExport
  def panel = div(width := 200, h1("EGIII"))
}
