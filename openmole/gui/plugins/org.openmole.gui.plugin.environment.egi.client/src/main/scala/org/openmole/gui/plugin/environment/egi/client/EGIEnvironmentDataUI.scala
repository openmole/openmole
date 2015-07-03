package org.openmole.gui.plugin.environment.egi.client

/*
 * Copyright (C) 02/07/2015 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.core.dataui.EnvironmentDataUI
import org.openmole.gui.client.core.ClientService
import org.openmole.gui.plugin.environment.egi.ext.EGIEnvironmentData
import scala.scalajs.js.annotation.JSExport
import rx._
import ClientService._

class EGIEnvironmentDataUI(val name: Var[String] = Var("")) extends EnvironmentDataUI {

  def data = new EGIEnvironmentData(name)

  def panelUI = new EGIEnvironmentPanelUI(this)

  def dataType = "EGI"
}