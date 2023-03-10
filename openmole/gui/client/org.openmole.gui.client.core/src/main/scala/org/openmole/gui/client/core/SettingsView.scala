package org.openmole.gui.client.core


import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom
import org.openmole.gui.client.ext.Utils.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.core.files.FileDisplayer
import com.raquo.laminar.api.L.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*

import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetIntervalHandle

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

object SettingsView:

  val jvmInfos: Var[Option[JVMInfos]] = Var(None)
  val timer: Var[Option[SetIntervalHandle]] = Var(None)

  //  lazy val dropdownApp = vForm(width := "auto",
  //    jvmInfoButton,
  //    docButton,
  //    jvmInfosDiv,
  //    resetPasswordButton,
  //    restartButton,
  //    shutdownButton
  //  ).dropdownWithTrigger(glyphSpan(glyph_menu_hamburger), omsheet.settingsBlock, Seq(left := "initial", right := 0))
  //
  //  lazy val dropdownConnection: Dropdown[_] = vForm(width := "auto")(
  //    resetPasswordButton
  //  ).dropdownWithTrigger(glyphSpan(glyph_menu_hamburger), omsheet.resetBlock, right := "20", left := "initial", right := 0)
  //
  private def serverActions(message: String, messageGlyph: HESetter, warnMessage: String, route: String)(using panels: Panels) =
    lazy val notification = panels.notifications.showAlternativeNotification(
      NotificationLevel.Info,
      warnMessage,
      div(),
      NotificationManager.Alternative("OK", _ => CoreUtils.setRoute(route)),
      NotificationManager.Alternative.cancel
    )

    button(btn_danger, marginBottom := "10px", message, onClick --> { _ ⇒ notification })

  def jvmInfoButton(using api: ServerAPI, basePath: BasePath) = button("JVM stats", btn_secondary, glyph_stats, onClick --> { _ ⇒
    timer.now() match {
      case Some(t) ⇒ stopJVMTimer(t)
      case _ ⇒ setJVMTimer
    }
  })

  def updateJVMInfos(using api: ServerAPI, basePath: BasePath) =
    api.jvmInfos().foreach { j ⇒
      jvmInfos.set(Some(j))
    }


  def setJVMTimer(using api: ServerAPI, basePath: BasePath) = {
    timer.set(Some(timers.setInterval(3000) {
      updateJVMInfos
    }))
  }

  def stopJVMTimer(t: SetIntervalHandle) = {
    timers.clearInterval(t)
    timer.set(None)
  }

  val waiter = timer.signal.map {
    _.isDefined
  }

  val jvmInfosDiv = waiter.expandDiv(div(
    generalSettings,
    child <-- jvmInfos.signal.map { oj ⇒
      val readableTotalMemory = oj.map{j=> CoreUtils.readableByteCount(j.totalMemory)}
      div(
        div(flexRow,
          div(cls := "bigValue", oj.map{j=> div(j.processorAvailable.toString)}.getOrElse(div())),
          div(cls := "smallText", "Processors")
        ),
        div(
          flexRow,
          div(cls := "bigValue", oj.map{j=> div(s"${(j.allocatedMemory.toDouble / j.totalMemory * 100).toInt}")}.getOrElse(div())),
          div(cls := "smallText", "Allocated memory (%)")
        ),
        div(
          flexRow,
          div(cls := "bigValue", readableTotalMemory.map{rm=> div(s"${CoreUtils.dropDecimalIfNull(rm.bytes)}")}.getOrElse(div())),
          div(cls := "smallText", readableTotalMemory.map{rm=> div(s"Total memory (${rm.units})")}.getOrElse(div()))
        ),
        oj.map{j=> div(s"${j.javaVersion}", cls := "smallText")}.getOrElse(div()),
        oj.map{j=> div(s"${j.jvmImplementation}", cls := "smallText")}.getOrElse(div()),
        oj.map{_=> div()}.getOrElse(Waiter.waiter.amend(flexRow, justifyContent.center))
      )
    }
  )
  )

  def resetPasswordButton(using panels: Panels) =
    serverActions(
      "Reset password",
      glyph_lock,
      "Careful! Resetting your password will wipe out all your preferences! Reset anyway?",
      s"/${resetPasswordRoute}"
    )

  def shutdownButton(using panels: Panels) = serverActions(
    "Shutdown",
    glyph_off,
    "This will stop the server, the application will no longer be usable. Halt anyway?",
    s"/${shutdownRoute}"
  )

  def restartButton(using panels: Panels) = serverActions(
    "Restart",
    glyph_repeat,
    "This will restart the server, the application will not respond for a while. Restart anyway?",
    s"/${restartRoute}"
  )

  def render(using api: ServerAPI, basePath: BasePath, panels: Panels) =
    div(cls := "settingButtons",
      jvmInfoButton,
      jvmInfosDiv,
      resetPasswordButton,
      shutdownButton,
      restartButton
    )

// val renderApp = dropdownApp

//val renderConnection = dropdownConnection

