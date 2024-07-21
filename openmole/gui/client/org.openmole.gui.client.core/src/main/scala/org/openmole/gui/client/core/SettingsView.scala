package org.openmole.gui.client.core


import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom
import org.openmole.gui.client.ext.ClientUtil.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.core.files.{FileDisplayer, FileToolBox}
import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.features.unitArrows
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*

import scala.scalajs.js.timers

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


  private def serverActions(message: String, messageGlyph: HESetter, warnMessage: String, route: String)(using panels: Panels) =
    lazy val notification = panels.notifications.showAlternativeNotification(
      NotificationLevel.Info,
      warnMessage,
      div(),
      NotificationManager.Alternative("OK", _ => CoreUtils.setRoute(route)),
      NotificationManager.Alternative.cancel
    )

    button(btn_danger, marginBottom := "10px", message, onClick --> { _ ⇒ notification })

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

  def jvmInfosDiv(using api: ServerAPI, basePath: BasePath) =
    div(
      generalSettings,
      child <--
        Signal.fromFuture(api.jvmInfos()).map:
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
    )

  def shutdownButton(using panels: Panels) = serverActions(
    "Shutdown",
    glyph_off,
    "This will stop the server, the application will no longer be usable. Halt anyway?",
    s"/${shutdownRoute}"
  )
  
  def removeContainerCacheButton(using api: ServerAPI, basePath: BasePath) =
    val disableButton = Var(false)
    button(
      btn_danger,
      marginBottom := "10px",
      "Remove Container Cache",
      disabled <-- disableButton,
      onClick --> { _ =>
        disableButton.set(true)
        api.removeContainerCache().andThen: _ =>
          disableButton.set(false)
      }
    )

  def dowloadAllFiles =
    import ServerFileSystemContext.Project
    a(div(fileActionItems, FileToolBox.glyphItemize(glyph_download), "Download All"), href := org.openmole.gui.shared.api.downloadFile(SafePath.root(Project), includeTopDirectoryInArchive = Some(false)))

  def render(using api: ServerAPI, basePath: BasePath, panels: Panels) =
    val jvmInfos: Var[Boolean] = Var(false)
    def jvmInfoButton(using api: ServerAPI, basePath: BasePath) =
      button("JVM stats", btn_secondary, marginBottom := "10px", glyph_stats, onClick --> jvmInfos.update(!_))

    div(cls := "settingButtons",
      version,
      jvmInfoButton,
      child <--
        jvmInfos.signal.map:
          case true => jvmInfosDiv
          case false => emptyNode,
      removeContainerCacheButton,
      shutdownButton,
      dowloadAllFiles
    )




