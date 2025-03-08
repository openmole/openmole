package org.openmole.gui.client.core

/*
 * Copyright (C) 23/07/15 // mathieu.leclaire@openmole.org
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

import org.openmole.core.market.*
import org.openmole.gui.shared.data.{ProcessState, Processing}
import org.openmole.gui.client.ext.*

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.bootstrapnative.bsn.*
import org.openmole.gui.client.core.CoreUtils.*
import org.openmole.gui.shared.data.*
import Waiter.*
import org.openmole.gui.client.core.files.TreeNodeManager
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.ext.InputFilter
import org.openmole.gui.shared.api.*


object MarketPanel:

  def render(using api: ServerAPI, basePath: BasePath, panels: Panels): Div =

    val marketIndex: Var[Option[MarketIndex]] = Var(None)
    api.marketIndex().foreach { m =>
      marketIndex.set(Some(m))
    }

    val tagFilter = InputFilter(pHolder = "Filter")
    val selectedEntry: Var[Option[MarketIndexEntry]] = Var(None)

    val overwriteAlert: Var[Option[MarketIndexEntry]] = Var(None)
    lazy val downloading: Var[Seq[(MarketIndexEntry, Var[_ <: ProcessState])]] = Var(marketIndex.now().map {
      _.entries.map {
        (_, Var(Processed()))
      }
    }.getOrElse(Seq()))

    def downloadButton(entry: MarketIndexEntry, todo: () => Unit = () => {}) =
      downloading.signal.map {
        _.find { _._1 == entry }.map {
          case (e, state: Var[ProcessState]) =>
            state.withTransferWaiter() { _ =>
              glyphSpan(Seq(btn_danger, glyph_download_alt), onClick --> { _ => todo() }).amend("Download")
            }
          case _ => div()
        }.getOrElse(div())
      }

    def overwrite(sp: SafePath, marketIndexEntry: MarketIndexEntry)(using api: ServerAPI, basePath: BasePath) =
      api.deleteFiles(Seq(sp)).foreach { d =>
        download(marketIndexEntry)
      }

    def exists(sp: SafePath, entry: MarketIndexEntry)(using api: ServerAPI, basePath: BasePath) =
      api.exists(sp).foreach { b =>
        if (b)
        then
          lazy val notif: NotificationManager.NotificationLine = panels.notifications.addAndShowNotificaton(
            NotificationLevel.Error,
            s"${entry.name}  already exists",
            div(btnGroup,
              button(btn_danger, "Overwrite"), onClick --> { _ =>
                overwriteAlert.set(None)
                overwrite(panels.treeNodePanel.treeNodeManager.directory.now() ++ entry.name, entry)
                panels.notifications.remove(notif)
                panels.closeExpandable
              },
              button(btn_secondary_outline, "Abort"), onClick --> { _ => overwriteAlert.set(None) }
            )
          )
          notif
        else 
          download(entry)
          panels.closeExpandable
      }

    def download(entry: MarketIndexEntry)(using api: ServerAPI, basePath: BasePath) =
      val manager = panels.treeNodePanel.treeNodeManager
      val path = manager.directory.now() ++ entry.name
      downloading.set(downloading.now().updatedFirst(_._1 == entry, (entry, Var(Processing()))))

      api.getMarketEntry(entry, path).foreach { d =>
        downloading.update(d => d.updatedFirst(_._1 == entry, (entry, Var(Processed()))))
        panels.treeNodePanel.refresh
      }

    def row(entry: MarketIndexEntry, i: Int, selected: Boolean) =
      val htmlDiv = div()
      entry.readme.foreach(htmlDiv.ref.innerHTML = _)

      div(flexRow,
        cls := "docEntry",
        backgroundColor := {
          if (i % 2 == 0) "#d1dbe4" else "#f4f4f4"
        },
        a(entry.name, float.left, color := "#222", width := "350px", cursor.pointer),
        entry.tags.map { e => span(cls := "badgeOM", e) }).expandOnclick(
        div(height := "200", backgroundColor := "#333", padding := "20", overflow.scroll,
          child <-- downloadButton(entry, () => {
            exists(panels.treeNodePanel.treeNodeManager.directory.now() ++ entry.name, entry)
          }),
          div(cls := "mdRendering", paddingTop := "40", htmlDiv, colSpan := 12)
        )
      )

    div(cls := "marketList",
      children <-- tagFilter.nameFilter.signal.combineWith(marketIndex.signal).combineWith(selectedEntry.signal).map { case (nf, marketIndex, sEntry) =>
        marketIndex.map { mindex =>
          mindex.entries.filter { e => tagFilter.exists(e.tags :+ e.name) }.zipWithIndex.map { case (entry, id) =>
            val isSelected = Some(entry) == sEntry
            row(entry, id, isSelected)
          }
        }.getOrElse(Seq(div()))
      }
    )



//      case (_, marketIndex, sEntry) =>

//    val marketPanel = new MarketPanel(manager)
//
//    marketPanel.overwriteAlert.signal.combineWith(manager.dirNodeLine.signal).map {
//      case (oAlert, current) =>
//        oAlert match {
//          case Some(e: MarketIndexEntry) =>
//            panels.alertPanel.string(
//              e.name + " already exists. Overwrite ? ",
//              () => {
//                marketPanel.overwriteAlert.set(None)
//                marketPanel.overwrite(current ++ e.name, e)
//              }, () => {
//                marketPanel.overwriteAlert.set(None)
//              }, CenterPagePosition
//            )
//            div
//          case _ =>
//        }
//    }
//    marketPanel
//
//class MarketPanel (manager: TreeNodeManager) {
//
//  private val marketIndex: Var[Option[MarketIndex]] = Var(None)
//  val tagFilter = InputFilter(pHolder = "Filter")
//  val selectedEntry: Var[Option[MarketIndexEntry]] = Var(None)
//

//
//
//  def marketTable(using api: ServerAPI) = div(
//    paddingTop := "20",
//    children <-- tagFilter.nameFilter.signal.combineWith(marketIndex.signal).combineWith(selectedEntry.signal).map {
//      case (_, marketIndex, sEntry) =>
//        marketIndex.map { mindex =>
//          mindex.entries.filter { e => tagFilter.exists(e.tags :+ e.name) }.flatMap { entry =>
//            val isSelected = Some(entry) == sEntry
//            Seq(
//              div(
//                cls := "docEntry",
//                div(colBS(3), (paddingTop := "7"),
//                  a(entry.name, cursor := "pointer", color := omsheet.WHITE,
//                    onClick --> { _ =>
//                      selectedEntry.set(
//                        if (isSelected) None
//                        else Some(entry)
//                      )
//                    })
//                ),
//                div(
//                  colBS(2),
//                  downloadButton(entry, () => {
//                    exists(manager.dirNodeLine.now() ++ entry.name, entry)
//                  })),
//                div(colBS(7), (paddingTop := "7"),
//                  label(entry.name, badge_primary, omsheet.tableTag)
//                ),
//                div(
//                  // if (isSelected)
//                  cls := "docEntry", //else emptyMod,
//                  sEntry.map { se =>
//                    if (isSelected) div(cls := "mdRendering", paddingTop := "40", se.readme, colSpan := 12)
//                    else div()
//                  }.getOrElse(div())
//                )
//              )
//            )
//          }
//        }.getOrElse(Seq())
//    }
//  )
//
//
//
//  def downloadButton(entry: MarketIndexEntry, todo: () => Unit = () => {}) = div()
//
//  //    downloading.signal.map {
//  //    _.find {
//  //      _._1 == entry
//  //    }.map {
//  //      case (e, state: Var[ProcessState]) =>
//  //        state.withTransferWaiter { _ =>
//  //          if (selectedEntry.now == Some(e)) glyphSpan(Seq(btn_danger, glyph_download_alt), onClick --> { _ => todo() }).amend("Download") else div()
//  //        }
//  //      case _ => div()
//  //    }.getOrElse(div())
//  //}
//
//
//  val marketHeader = span(b("Market place"))
//
//  def marketBody(using api: ServerAPI) = div(
//    tagFilter.tag,
//    marketTable
//  )
//
//  def marketFooter(using api: ServerAPI) = closeButton("Close", () => modalDialog.hide).amend(btn_secondary)
//
//  def modalDialog(using api: ServerAPI): ModalDialog = ModalDialog(
//    marketHeader,
//    marketBody,
//    marketFooter,
//    omsheet.panelWidth(92),
//    onopen = () => {
//      marketIndex.now() match {
//        case None =>
//          api.marketIndex().foreach { m =>
//            marketIndex.set(Some(m))
//          }
//        case _ =>
//      }
//    },
//    onclose = () => {}
//  )
//
//

//
//}
