//package org.openmole.gui.client.core
//
//import scaladget.bootstrapnative.bsn._
//import org.openmole.gui.client.ext._
//import com.raquo.laminar.api.L._
//
///*
// * Copyright (C) 07/11/16 // mathieu.leclaire@openmole.org
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
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//class Connection {
//
//  lazy val connectButton = button("Connect", btn_primary, `type` := "submit")
//
//  val passwordInput = inputTag("").amend(
//    placeholder := "Password",
//    `type` := "password",
//    width := "130px",
//    marginBottom := "15",
//    nameAttr := "password",
//    onMountFocus
//  )
//
//  def cleanInputs = {
//    passwordInput.ref.value = ""
//  }
//
//  private val connectionForm = form(
//    method := "post",
//    cls := "connection-form",
//    passwordInput,
//    connectButton
//  )
//
//  val render = {
//    //panels.settingsView.renderConnection,
//    div(
//      cls := "screen-center",
//      img(src := "img/openmole_light.png", width := "600px"),
//      connectionForm
//    )
//
//  }
//}
