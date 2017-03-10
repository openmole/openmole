package org.openmole.gui.client.core

import fr.iscpif.scaladget.stylesheet.all._
import org.openmole.gui.client.core.alert.AbsolutePositioning.CenterPagePosition
import org.openmole.gui.client.core.alert.AlertPanel
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.client.core.panels._
import org.scalajs.dom
import rx._
import bs._
import fr.iscpif.scaladget.api.Selector.Dropdown
import org.openmole.gui.ext
import org.openmole.gui.ext.data.routes
import org.openmole.gui.ext.tool.client._

import scalatags.JsDom.all._

/*
 * Copyright (C) 07/11/16 // mathieu.leclaire@openmole.org
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

object SettingsView {

  private def alertPanel(warnMessage: String, route: String) = AlertPanel.string(
    warnMessage,
    () ⇒ {
      fileDisplayer.tabs.saveAllTabs(() ⇒
        dom.window.location.href = route)
    },
    transform = CenterPagePosition
  )

  lazy val dropdownApp: Dropdown[_] = bs.vForm(width := "auto")(
    resetPasswordButton,
    restartButton,
    shutdownButton
  ).dropdownWithTrigger(bs.glyphSpan(glyph_menu_hamburger), omsheet.resetBlock, Seq(left := "initial", right := 0))

  lazy val dropdownConnection: Dropdown[_] = bs.vForm(width := "auto")(
    resetPasswordButton
  ).dropdownWithTrigger(bs.glyphSpan(glyph_menu_hamburger), omsheet.resetBlock +++ (right := 20), Seq(left := "initial", right := 0))

  private def serverActions(message: String, messageGlyph: Glyphicon, warnMessage: String, route: String) =
    div(rowLayout +++ (lineHeight := "7px"))(
      bs.glyphSpan(messageGlyph +++ omsheet.shutdownButton +++ columnLayout),
      span(message, settingsItemStyle +++ columnLayout +++ (paddingAll(top = 3, left = 5))),
      onclick := { () ⇒
        dropdownApp.close
        dropdownConnection.close
        alertPanel(warnMessage, route)
      }
    ).render

  val resetPasswordButton =
    serverActions(
      "reset password",
      glyph_lock,
      "Careful! Resetting your password will wipe out all your preferences! Reset anyway?",
      routes.resetPasswordRoute
    )

  val shutdownButton = serverActions(
    "shutdown",
    glyph_off,
    "This will stop the server, the application will no longer be usable. Halt anyway?",
    routes.shutdownRoute
  )

  val restartButton = serverActions(
    "restart",
    glyph_repeat,
    "This will restart the server, the application will not respond for a while. Restart anyway?",
    routes.restartRoute
  )

  val renderApp = dropdownApp.render

  val renderConnection = dropdownConnection.render

}
