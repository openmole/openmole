package org.openmole.gui.client.core

import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.ext.data._
import org.openmole.gui.client.tool.OMTags
import org.scalajs.dom.raw.{ HTMLInputElement, MouseEvent }
import scalatags.JsDom.all._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import scalatags.JsDom.tags

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.ext.client._
import autowire._
import rx._
import org.openmole.gui.client.core.alert.BannerAlert
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.client.FileManager

import scala.concurrent.duration.DurationInt

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

class PluginPanel(bannerAlert: BannerAlert) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  case class IndexedPlugin(plugin: Plugin, index: Int)

  implicit def indexToPlugin(i: Int): Option[IndexedPlugin] = plugins.now.find(p ⇒ p.index == i)

  implicit def seqIndexToSeqPlugin(s: Seq[Int]): Seq[IndexedPlugin] = s.map {
    indexToPlugin
  }.flatten

  private lazy val plugins: Var[Seq[IndexedPlugin]] = Var(Seq())
  lazy val transferring: Var[ProcessState] = Var(Processed())
  private val selected: Var[Seq[IndexedPlugin]] = Var(Seq())

  def getPlugins = {
    Post()[Api].listPlugins.call().foreach { a ⇒
      plugins() = a.toSeq.zipWithIndex.map { x ⇒ IndexedPlugin(x._1, x._2) }
    }
  }

  val uploadPluginButton = tags.label(
    pluginRight +++ uploadPlugin +++ "inputFileStyle",
    OMTags.uploadButton((fileInput: HTMLInputElement) ⇒ {
      fileInput.accept = ".jar"

      val directoryName = s"uploadPlugin${java.util.UUID.randomUUID().toString}"

      FileManager.upload(
        fileInput,
        SafePath.empty,
        (p: ProcessState) ⇒ { transferring() = p },
        UploadPlugin(directoryName),
        () ⇒ {
          val plugins = FileManager.fileNames(fileInput.files)
          Post(timeout = 5 minutes)[Api].addUploadedPlugins(directoryName, plugins).call().foreach { ex ⇒
            if (ex.isEmpty) getPlugins
            else {
              dialog.hide
              plugins.foreach { p ⇒ Post()[Api].removePlugin(Plugin(p)).call() }
              bannerAlert.registerWithDetails("Plugin import failed", ErrorData.stackTrace(ex.head))
            }
          }
        }
      )
    })
  ).tooltip("Upload plugin")

  val deleteButton = buttonIcon("", btn_danger, glyph_trash, todo = () ⇒ {
    selected.now.foreach { p ⇒
      removePlugin(p.plugin)
    }
  })

  lazy val pluginTable = {

    case class Reactive(p: IndexedPlugin) {
      val lineHovered: Var[Boolean] = Var(false)

      lazy val render =
        Rx {
          div(
            docEntry ++ (
              if (selected().contains(p)) backgroundColor := "#87bede"
              else emptyMod
            ))(
              span(p.plugin.name, docTitleEntry +++ floatLeft),
              span(p.plugin.time, dateStyle),
              onselect := { () ⇒ false },
              onclick := { (e: MouseEvent) ⇒

                val selectedIndex = selected.now.map {
                  _.index
                }
                val range = {
                  if (e.shiftKey) {
                    if (!selected.now.isEmpty) {
                      val preSel = (
                        if (selectedIndex.contains(p.index)) selectedIndex.filterNot {
                          _ > p.index
                        }
                        else p.index +: selectedIndex
                      ).sorted
                      Seq.range(preSel.head, preSel.last + 1, 1)
                    }
                    else if (!selectedIndex.contains(p.index)) selectedIndex :+ p.index
                    else selectedIndex.filterNot(_ == p.index)
                  }
                  else if (!selectedIndex.contains(p.index)) selectedIndex :+ p.index
                  else selectedIndex.filterNot(_ == p.index)
                }

                val selectedPlugins: Seq[IndexedPlugin] = range

                selected() = selectedPlugins
                e.preventDefault()
              }
            )
        }
    }

    div(
      div(spinnerStyle)(
        transferring.withTransferWaiter { _ ⇒
          tags.div()
        }
      ),
      Rx {
        plugins().map {
          Reactive(_).render
        }
      }
    )

  }

  def removePlugin(plugin: Plugin) =
    Post()[Api].removePlugin(plugin).call().foreach {
      p ⇒
        getPlugins
    }

  lazy val dialog = ModalDialog(onopen = () ⇒ getPlugins)

  dialog.header(
    tags.span(
      tags.b("Plugins"),
      inputGroup(navbar_right)(
        uploadPluginButton
      )
    )
  )

  dialog.body(pluginTable)

  dialog.footer(
    tags.div(
      buttonGroup()(
        Rx {
          if (selected().isEmpty) span
          else deleteButton
        },
        ModalDialog.closeButton(dialog, btn_default, "Close")
      )
    )
  )

}