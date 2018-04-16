package org.openmole.gui.client.core

import org.openmole.gui.client.core.alert.AbsolutePositioning.CenterPagePosition

import scalatags.JsDom._
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.panels._
import org.scalajs.dom
import org.openmole.gui.ext.tool.client.Utils._

import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.{ JVMInfos, routes }
import org.openmole.gui.ext.tool.client._
import autowire._
import rx._

import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetIntervalHandle
import scaladget.bootstrapnative.Selector.Dropdown
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

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val jvmInfos: Var[Option[JVMInfos]] = Var(None)
  val timer: Var[Option[SetIntervalHandle]] = Var(None)

  private def alertPanel(warnMessage: String, route: String) = AlertPanel.string(
    warnMessage,
    () ⇒ {
      fileDisplayer.tabs.saveAllTabs(() ⇒
        dom.window.location.href = route)
    },
    transform = CenterPagePosition
  )

  lazy val dropdownApp: Dropdown[_] = vForm(width := "auto")(
    jvmInfoButton,
    docButton,
    jvmInfosDiv,
    resetPasswordButton.render,
    restartButton,
    shutdownButton
  ).dropdownWithTrigger(glyphSpan(glyph_menu_hamburger), omsheet.settingsBlock, Seq(left := "initial", right := 0))

  lazy val dropdownConnection: Dropdown[_] = vForm(width := "auto")(
    resetPasswordButton.render
  ).dropdownWithTrigger(glyphSpan(glyph_menu_hamburger), omsheet.resetBlock +++ (right := 20), Seq(left := "initial", right := 0))

  private def serverActions(message: String, messageGlyph: Glyphicon, warnMessage: String, route: String) =
    div(rowLayout +++ (lineHeight := "7px"))(
      glyphSpan(messageGlyph +++ omsheet.shutdownButton +++ columnLayout),
      span(message, Seq(paddingTop := 3, paddingLeft := 5) +++ settingsItemStyle +++ columnLayout),
      onclick := { () ⇒
        dropdownApp.close
        dropdownConnection.close
        alertPanel(warnMessage, route)
      }
    )

  val docButton =
    div(a(href := "https://next.openmole.org/GUI+guide.html")(target := "_blank", tags.span("Documentation"))).render

  val jvmInfoButton = button("JVM stats", btn_default +++ (marginLeft := 12), glyph_stats, onclick := { () ⇒
    timer.now match {
      case Some(t) ⇒ stopJVMTimer(t)
      case _       ⇒ setJVMTimer
    }
  }).render

  def updateJVMInfos = {
    post()[Api].jvmInfos.call().foreach { j ⇒
      jvmInfos() = Some(j)
    }
  }

  def setJVMTimer = {
    timer() = Some(timers.setInterval(3000) {
      updateJVMInfos
    })
  }

  def stopJVMTimer(t: SetIntervalHandle) = {
    timers.clearInterval(t)
    timer() = None
  }

  val waiter = timer.map {
    _.isDefined
  }

  val jvmInfosDiv = timer.map {
    _.isDefined
  }.expandDiv(tags.div(generalSettings)(
    Rx {
      for (
        j ← jvmInfos()
      ) yield {
        val readableTotalMemory = CoreUtils.readableByteCount(j.totalMemory)
        tags.div(
          tags.div(highLine)(
            tags.div(bigHalfColumn)(j.processorAvailable.toString),
            tags.div(smallHalfColumn)("Processors")
          ),
          tags.div(highLine)(
            tags.div(bigHalfColumn)(s"${(j.allocatedMemory.toDouble / j.totalMemory * 100).toInt}"),
            tags.div(smallHalfColumn)("Allocated memory (%)")
          ),
          tags.div(highLine)(
            tags.div(bigHalfColumn)(s"${CoreUtils.dropDecimalIfNull(readableTotalMemory.bytes)}"),
            tags.div(smallHalfColumn)(s"Total memory (${readableTotalMemory.units})")
          ),
          tags.div(smallLine)(
            tags.div(smallHalfColumn +++ textCenter +++ (paddingTop := 5))(s"${j.javaVersion}")
          ),
          tags.div(smallLine)(
            tags.div(smallHalfColumn +++ textCenter)(s"${j.jvmImplementation}")
          )
        )
      }
    }
  ))

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
  ).render

  val restartButton = serverActions(
    "restart",
    glyph_repeat,
    "This will restart the server, the application will not respond for a while. Restart anyway?",
    routes.restartRoute
  ).render

  val renderApp = dropdownApp.render

  val renderConnection = dropdownConnection.render

}
