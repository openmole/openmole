package org.openmole.gui.client.core

import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.ext.data._
import org.openmole.gui.client.tool.OMTags
import org.scalajs.dom.raw.HTMLInputElement

import scalatags.JsDom.all._
import scaladget.api.{ BootstrapTags ⇒ bs }

import scalatags.JsDom.tags
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.ext.tool.client.JsRxTags._
import org.openmole.gui.ext.tool.client._
import autowire._
import rx._
import bs._
import scaladget.stylesheet.{ all ⇒ sheet }
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.tool.client.FileManager
import sheet._

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

class PluginPanel {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  private lazy val plugins: Var[Option[Seq[Plugin]]] = Var(None)
  lazy val transferring: Var[ProcessState] = Var(Processed())

  def getPlugins = {
    post()[Api].listPlugins.call().foreach { a ⇒
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
        () ⇒
          post()[Api].addPlugins(FileManager.fileNames(fileInput.files)).call().foreach { ex ⇒
            if (ex.isEmpty) getPlugins
            else AlertPanel.detail("Plugin import failed", ex.head.stackTrace)
          }
      )
    })
  ).tooltip("Upload plugin")

  lazy val pluginTable = {

    case class Reactive(p: Plugin) {
      val lineHovered: Var[Boolean] = Var(false)

      lazy val render =
        tags.div(
          docEntry
        )(
          span(p.name, docTitleEntry +++ floatLeft),
          span(bs.glyphSpan(glyph_trash, () ⇒ removePlugin(p))(grey +++ sheet.paddingTop(10) +++ sheet.paddingLeft(10) +++ "glyphitem" +++ glyph_trash)),
          span(p.time, dateStyle)
        )
    }

    div(
      div(spinnerStyle)(
        transferring.withTransferWaiter { _ ⇒
          tags.div()
        }
      ),
      Rx {
        div(
          plugins().map { aux ⇒
            for (a ← aux) yield {
              Seq(Reactive(a).render)
            }
          }
        )
      }
    )

  }

  def removePlugin(plugin: Plugin) =
    post()[Api].removePlugin(plugin).call().foreach {
      p ⇒
        getPlugins
    }

  lazy val dialog = bs.ModalDialog(onopen = () ⇒ getPlugins)

  dialog.header(
    tags.span(
      tags.b("Plugins"),
      bs.inputGroup(navbar_right)(
        uploadPluginButton
      )
    )
  )

  dialog.body(pluginTable)

  dialog.footer(
    tags.div(
      bs.ModalDialog.closeButton(dialog, btn_default, "Close")
    )
  )

}