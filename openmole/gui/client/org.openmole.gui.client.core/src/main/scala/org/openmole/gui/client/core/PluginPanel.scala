package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.{ Transfered, Standby, FileTransferState, FileManager }
import org.openmole.gui.ext.data.{ UploadPlugin, SafePath, Plugin }
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.HTMLInputElement
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import bs._
import rx._

/*
 * Copyright (C) 10/08/15 // mathieu.leclaire@openmole.org
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

class PluginPanel extends ModalPanel {
  lazy val modalID = "pluginPanelID"

  private val plugins: Var[Option[Seq[Plugin]]] = Var(None)
  val transferring: Var[FileTransferState] = Var(Standby())

  def onOpen() = {
    getPlugins
  }

  def onClose() = {
    println("clossssssse")
  }

  def getPlugins = {
    OMPost[Api].listPlugins.call().foreach { a ⇒
      plugins() = Some(a.toSeq)
    }
  }

  val uploadPluginButton = tags.label(`class` := "inputFileStyle pluginRight",
    uploadButton((fileInput: HTMLInputElement) ⇒ {
      fileInput.accept = ".jar"
      FileManager.upload(fileInput,
        SafePath.empty,
        (p: FileTransferState) ⇒ {
          println("Transfer: " + p.ratio + " " + p.display)
          transferring() = p
        },
        UploadPlugin(),
        () ⇒ getPlugins
      )
    })).tooltip("Upload plugin")

  lazy val pluginTable = {

    case class Reactive(p: Plugin) {
      val lineHovered: Var[Boolean] = Var(false)

      val render = Rx {
        bs.tr(row)(
          onmouseover := { () ⇒
            lineHovered() = true
          },
          onmouseout := { () ⇒
            lineHovered() = false
          },
          tags.td(
            tags.i(p.name, `class` := "left", cursor := "pointer")
          ),
          tags.td(id := Rx {
            "treeline" + {
              if (lineHovered()) "-hover" else ""
            }
          })(
            glyphSpan(glyph_trash, () ⇒ removePlugin(p))(id := "glyphtrash", `class` := "glyphitem grey")
          )
        )
      }
    }

    Rx {
      tags.div(
        transferring() match {
          case _: Standby ⇒
          case _: Transfered ⇒
            getPlugins
            transferring() = Standby()
          case _ ⇒
            println("transferring " + transferring())
            progressBar(transferring().display, transferring().ratio)(id := "treeprogress")
        },
        bs.table(striped)(
          thead,
          tbody(
            plugins().map { aux ⇒
              for (a ← aux) yield {
                Seq(Reactive(a).render)
              }
            }
          )
        )
      )
    }
  }

  def removePlugin(plugin: Plugin) =
    OMPost[Api].removePlugin(plugin).call().foreach { p ⇒
      getPlugins
    }

  val dialog = modalDialog(
    modalID,
    headerDialog(Rx {
      tags.span(tags.b("Plugins"),
        inputGroup(navbar_right)(
          uploadPluginButton
        ))
    }),
    bodyDialog(pluginTable),
    footerDialog(
      tags.div(
        closeButton
      )
    )
  )

}