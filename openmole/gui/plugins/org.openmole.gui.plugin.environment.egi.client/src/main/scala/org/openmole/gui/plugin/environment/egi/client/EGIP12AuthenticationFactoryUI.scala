package org.openmole.gui.plugin.environment.egi.client

import org.openmole.gui.ext.data.EGIP12AuthenticationData
import scala.scalajs.js.annotation.JSExport
import org.openmole.gui.ext.dataui.{ AuthenticationFactoryUI, PanelUI }

/*
 * Copyright (C) 02/07/15 // mathieu.leclaire@openmole.org
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

@JSExport("org.openmole.gui.plugin.environment.egi.client.EGIP12AuthenticationFactoryUI")
class EGIP12AuthenticationFactoryUI extends AuthenticationFactoryUI {

  val name = "EGI P12 certificate"
  type DATA = EGIP12AuthenticationData

  def panelUI(data: DATA): PanelUI = new EGIP12AuthenticationPanelUI(data)

  def data: DATA = EGIP12AuthenticationData()
}
