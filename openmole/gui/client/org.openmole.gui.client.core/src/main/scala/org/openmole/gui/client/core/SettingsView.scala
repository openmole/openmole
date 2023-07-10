package org.openmole.gui.client.core


import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom
import org.openmole.gui.client.ext.ClientUtil.*
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

  def version(using api: ServerAPI, basePath: BasePath) =
    div(color := "#333", marginBottom := "30px",
      child <-- Signal.fromFuture(api.omSettings().map { sets ⇒
        div(flexColumn,
          div(sets.version, color:= "#3086b5", fontSize := "22px", fontWeight.bold),
          div(sets.versionName),
          div(CoreUtils.longTimeToString(sets.buildTime))
        )
      }).map {
        case Some(e) => e
        case _ => Waiter.waiter
      }
    )

  val jvmInfosDiv =
    timer.signal.map {
      _.isDefined
    }.expand(
      div(
        generalSettings,
        child <-- jvmInfos.signal.map {
          _ match {
            case Some(j) =>
              val readableTotalMemory = CoreUtils.readableByteCount(j.totalMemory)
              div(flexColumn,
                div(flexRow,
                  div(cls := "smallText", "Processors"),
                  div(cls := "bigValue", div(j.processorAvailable.toString))
                ),
                div(
                  flexRow,
                  div(cls := "smallText", "Allocated memory (%)"),
                  div(cls := "bigValue", div(s"${(j.allocatedMemory.toDouble / j.totalMemory * 100).toInt}"))
                ),
                div(
                  flexRow,
                  div(cls := "smallText", div(s"Total memory (${readableTotalMemory.units})")),
                  div(cls := "bigValue", div(s"${CoreUtils.dropDecimalIfNull(readableTotalMemory.bytes)}"))
                ),
                div(
                  flexRow,
                  div(cls := "smallText", "Java version"),
                  div(flexColumn,
                    div(s"${j.javaVersion}", cls := "bigValue"),
                    div(s"${j.jvmImplementation}", cls := "bigValue", fontSize := "22", textAlign.right)
                  ),
                )
              )
            case _ => Waiter.waiter.amend(flexRow, justifyContent.center)
          }
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
      version,
      jvmInfoButton,
      jvmInfosDiv,
      resetPasswordButton,
      shutdownButton,
      restartButton
    )

