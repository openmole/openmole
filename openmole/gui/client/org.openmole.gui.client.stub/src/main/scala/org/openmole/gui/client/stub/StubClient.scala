package org.openmole.gui.client.stub

import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.*
import org.openmole.gui.client.ext.{NotificationLevel, PluginServices, STTPInterpreter}

import scala.scalajs.js.annotation.*

/*
 * Copyright (C) 2023 Romain Reuillon
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

@JSExportTopLevel(name = "openmole_stub_client") @JSExportAll
object StubClient:
  lazy val panels = Panels()
  lazy val sttp = STTPInterpreter()
  lazy val api = OpenMOLERESTServerAPI(sttp, NotificationManager.toService(panels.notifications))

  lazy val pluginServices =
    PluginServices(
      errorManager = (message, stack) => panels.notifications.showGetItNotification(NotificationLevel.Error, message, div(stack))
    )

  val gui = OpenMOLEGUI(using panels, pluginServices, api)

  export gui.*



