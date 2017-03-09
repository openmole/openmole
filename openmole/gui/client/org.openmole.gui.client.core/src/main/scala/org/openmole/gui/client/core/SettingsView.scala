package org.openmole.gui.client.core

import fr.iscpif.scaladget.stylesheet.all._
import org.openmole.gui.client.core.alert.AbsolutePositioning.CenterPagePosition
import org.openmole.gui.client.core.alert.AlertPanel
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.client.core.panels._
import org.scalajs.dom
import rx._
import bs._
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

class SettingsView {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val alert: Var[Boolean] = Var(false)
  alert.triggerLater {
    alertPanel
  }

  def alertPanel = AlertPanel.string(
    "Careful! Resetting your password will wipe out all your preferences! Reset anyway?",
    () ⇒ {
      alert() = false
      dom.window.location.href = "resetPassword"
    }, () ⇒ {
      alert() = false
    }, CenterPagePosition
  )

  val resetPassword = span(onclick := { () ⇒
    alert() = true
  }, omsheet.settingsItemStyle)("Reset password").render

  val shutdownButton = div(rowLayout +++ (lineHeight := "7px"))(
    bs.glyphSpan(glyph_off +++ omsheet.shutdownButton +++ columnLayout),
    span("Shutdown", settingsItemStyle +++ columnLayout +++ (scalatags.JsDom.all.paddingTop := 8)),
    onclick := { () ⇒
      AlertPanel.string(
        "This will stop the server, the application will no longer be usable. Halt anyway?",
        () ⇒ {
          fileDisplayer.tabs.saveAllTabs(() ⇒
            dom.window.location.href = "shutdown")
        },
        transform = CenterPagePosition
      )
    }
  ).render

  val renderApp = bs.vForm(width := "auto")(
    resetPassword,
    shutdownButton
  ).dropdownWithTrigger(bs.glyphSpan(glyph_menu_hamburger), omsheet.resetBlock, Seq(left := "initial", right := 0)).render

  val renderConnection = bs.vForm(width := "auto")(
    resetPassword
  ).dropdownWithTrigger(bs.glyphSpan(glyph_menu_hamburger), omsheet.resetBlock +++ (right := 20), Seq(left := "initial", right := 0)).render

}
