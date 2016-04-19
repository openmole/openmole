package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.FileManager
import org.openmole.gui.misc.js.Tooltip._
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.utils.stylesheet._
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.HTMLInputElement
import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.OMTags
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.misc.js.JsRxTags._
import autowire._
import rx._
import bs._
import org.openmole.gui.misc.utils.{ stylesheet ⇒ sheet }
import fr.iscpif.scaladget.stylesheet.all._

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
  val transferring: Var[ProcessState] = Var(Processed())

  def onOpen() = {
    getPlugins
  }

  def onClose() = {
  }

  def getPlugins = {
    OMPost[Api].listPlugins.call().foreach { a ⇒
      plugins() = Some(a.toSeq)
    }
  }

  val uploadPluginButton = tags.label(
    pluginRight +++ uploadPlugin +++ "inputFileStyle",
    OMTags.uploadButton((fileInput: HTMLInputElement) ⇒ {
      fileInput.accept = ".jar"
      FileManager.upload(
        fileInput,
        SafePath.empty,
        (p: ProcessState) ⇒ { transferring() = p },
        UploadPlugin(),
        () ⇒ getPlugins
      )
    })
  ).tooltip("Upload plugin")

  lazy val pluginTable = {

    case class Reactive(p: Plugin) {
      val lineHovered: Var[Boolean] = Var(false)

      val render = Rx {
        tags.div(
          `class` := "docEntry",
          onmouseover := { () ⇒
            lineHovered() = true
          },
          onmouseout := { () ⇒
            lineHovered() = false
          }
        )(
            span(p.name, `class` := "left docTitleEntry"),
            span(
              Rx {
                if (lineHovered()) opaque
                else transparent
              },
              glyphSpan(glyph_trash, () ⇒ removePlugin(p))(id := "glyphtrash", `class` := "glyphitem grey spacer9")
            )
          )
      }
    }

    Rx {
      div(
        transferring() match {
          case _: Processed ⇒
            getPlugins
            transferring() = Processed()
          case _ ⇒
            bs.progressBar(transferring().display, transferring().ratio)(treeprogress)
        },
        div(
          plugins().map { aux ⇒
            for (a ← aux) yield {
              Seq(Reactive(a).render)
            }
          }
        )
      )
    }
  }

  def removePlugin(plugin: Plugin) =
    OMPost[Api].removePlugin(plugin).call().foreach { p ⇒
      getPlugins
    }

  val dialog = bs.modalDialog(
    modalID,
    bs.headerDialog(Rx {
      tags.span(
        tags.b("Plugins"),
        bs.inputGroup(navbar_right)(
          uploadPluginButton
        )
      )
    }),
    bs.bodyDialog(pluginTable),
    bs.footerDialog(
      tags.div(
        closeButton
      )
    )
  )

}