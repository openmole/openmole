//package org.openmole.gui.client.stub
//
//import org.openmole.gui.client.core.*
//import org.openmole.gui.client.ext.{NotificationLevel, PluginServices}
//
//import scala.scalajs.js.annotation.*
//import com.raquo.laminar.api.L.*
//
///*
// * Copyright (C) 2023 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//@JSExportTopLevel(name = "openmole_stub") @JSExportAll
//object Stub:
//  lazy val api = AnimatedStubRESTServerAPI()
//
//  lazy val pluginServices =
//    PluginServices(
//      errorManager = (message, stack) => panels.notifications.showGetItNotification(NotificationLevel.Error, message, div(stack))
//    )
//
//  lazy val panels = Panels()
//
//  val gui = OpenMOLEGUI(using panels, pluginServices, api)
//
//  export gui.*
//
//
