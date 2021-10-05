package org.openmole.gui.client.core

import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.ext.data._
import org.openmole.gui.client.tool.OMTags
import org.scalajs.dom.raw.{ HTMLInputElement, MouseEvent }
import scaladget.bootstrapnative.bsn._
import scaladget.tools._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.ext.client._
import autowire._
import org.openmole.gui.client.core.alert.BannerAlert
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.client.FileManager
import com.raquo.laminar.api.L._
import scaladget.bootstrapnative.bsn._

import scala.concurrent.duration.DurationInt

//
///*
// * Copyright (C) 10/08/15 // mathieu.leclaire@openmole.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
class PluginPanel(bannerAlert: BannerAlert) {

  //
  case class IndexedPlugin(plugin: Plugin, index: Int)

  implicit def indexToPlugin(i: Int): Option[IndexedPlugin] = plugins.now.find(p ⇒ p.index == i)

  implicit def seqIndexToSeqPlugin(s: Seq[Int]): Seq[IndexedPlugin] = s.map {
    indexToPlugin
  }.flatten

  private lazy val plugins: Var[Seq[IndexedPlugin]] = Var(Seq())
  lazy val transferring: Var[ProcessState] = Var(Processed())
  val selected: Var[Seq[IndexedPlugin]] = Var(Seq())

  //
  def getPlugins = {
    Post()[Api].listPlugins.call().foreach { a ⇒
      plugins.set(a.toSeq.zipWithIndex.map { x ⇒ IndexedPlugin(x._1, x._2) })
    }
  }

  val uploadPluginButton = label(
    pluginRight, uploadPlugin, "inputFileStyle",
    OMTags.uploadButton((fileInput: Input) ⇒ {
      fileInput.ref.accept = ".jar"

      val directoryName = s"uploadPlugin${java.util.UUID.randomUUID().toString}"

      FileManager.upload(
        fileInput,
        SafePath.empty,
        (p: ProcessState) ⇒ {
          transferring.set(p)
        },
        UploadPlugin(directoryName),
        () ⇒ {
          val plugins = FileManager.fileNames(fileInput.ref.files)
          Post(timeout = 5 minutes)[Api].addUploadedPlugins(directoryName, plugins).call().foreach { ex ⇒
            if (ex.isEmpty) getPlugins
            else {
              pluginDialog.hide
              plugins.foreach { p ⇒ Post()[Api].removePlugin(Plugin(p)).call() }
              bannerAlert.registerWithDetails("Plugin import failed", ErrorData.stackTrace(ex.head))
            }
          }
        }
      )
    })
  ).tooltip("Upload plugin")

  val deleteButton = button("", btn_danger, glyph_trash, onClick --> { _ ⇒
    selected.now.foreach { p ⇒
      removePlugin(p.plugin)
    }
  })

  lazy val pluginTable = {
    // Rx {
    div(
      div(
        spinnerStyle,
        transferring.withTransferWaiter { _ ⇒
          div()
        }
      ),
      div(
        docEntry,
        backgroundColor <-- selected.signal.map { s ⇒
          if (s.contains(p)) "#87bede"
          else "#fffff00"
        },
        children <-- selected.signal.combineWith(plugins.signal).map {
          case (s, ps) ⇒
            ps.map { p ⇒
              div(
                docEntry ++ (
                  if (s.contains(p)) backgroundColor := "#87bede"
                  else emptySetters
                ),
                span(p.plugin.name, docTitleEntry, float.left),
                span(p.plugin.time, dateStyle),
                onSelect --> { _ ⇒ false },
                onClick --> { (e: MouseEvent) ⇒
                  val selectedIndex = s.map {
                    _.index
                  }
                  val range = {
                    if (e.shiftKey) {
                      if (!s.isEmpty) {
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

                  selected.set(selectedPlugins)
                  e.preventDefault()
                }
              )
            }
        }
      )
    )
  }

  def removePlugin(plugin: Plugin) =
    Post()[Api].removePlugin(plugin).call().foreach {
      p ⇒
        getPlugins
    }

  val dialogHeader = span(
    b("Plugins"),
    inputGroupAppend(uploadPluginButton)
  )

  val dialogBody = pluginTable

  val dialogFooter =
    div(
      buttonGroup.amend(
        child <-- selected.signal.map { s ⇒
          if (s.isEmpty) div()
          else deleteButton
        },
        closeButton("Close")
      )
    )

  lazy val pluginDialog: ModalDialog = ModalDialog(
    dialogHeader,
    dialogBody,
    dialogFooter,
    emptySetters,
    onopen = () ⇒ getPlugins,
    onclose = () ⇒ {}
  )

}