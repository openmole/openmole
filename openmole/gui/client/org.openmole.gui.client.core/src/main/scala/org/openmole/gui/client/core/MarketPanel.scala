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

import org.openmole.core.market._
import org.openmole.gui.client.core.alert.{ AbsolutePositioning, AlertPanel }
import AbsolutePositioning.CenterPagePosition
import org.openmole.gui.ext.data.{ ProcessState, Processing }
import org.openmole.gui.ext.client._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import scaladget.bootstrapnative.bsn._
import org.openmole.gui.client.core.CoreUtils._
import org.openmole.gui.ext.data._
import Waiter._
import autowire._
import org.openmole.gui.client.core.files.TreeNodeManager

import com.raquo.laminar.api.L._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.client.InputFilter

class MarketPanel(manager: TreeNodeManager) {

  private val marketIndex: Var[Option[MarketIndex]] = Var(None)
  val tagFilter = InputFilter(pHolder = "Filter")
  val selectedEntry: Var[Option[MarketIndexEntry]] = Var(None)
  lazy val downloading: Var[Seq[(MarketIndexEntry, Var[_ <: ProcessState])]] = Var(marketIndex.now.map {
    _.entries.map {
      (_, Var(Processed()))
    }
  }.getOrElse(Seq()))
  val overwriteAlert: Var[Option[MarketIndexEntry]] = Var(None)

  overwriteAlert.signal.combineWith(manager.current.signal).map {
    case (oAlert, current) ⇒
      oAlert match {
        case Some(e: MarketIndexEntry) ⇒
          panels.alertPanel.string(
            e.name + " already exists. Overwrite ? ",
            () ⇒ {
              overwriteAlert.set(None)
              deleteFile(current ++ e.name, e)
            }, () ⇒ {
              overwriteAlert.set(None)
            }, CenterPagePosition
          )
          div
        case _ ⇒
      }
  }

  lazy val marketTable = div(
    paddingTop := "20",
    children <-- tagFilter.nameFilter.signal.combineWith(marketIndex.signal).combineWith(selectedEntry.signal).map {
      case (_, marketIndex, sEntry) ⇒
        marketIndex.map { mindex ⇒
          mindex.entries.filter { e ⇒ tagFilter.exists(e.tags :+ e.name) }.flatMap { entry ⇒
            val isSelected = Some(entry) == sEntry
            Seq(
              div(
                omsheet.docEntry,
                div(colBS(3), (paddingTop := "7"),
                  a(entry.name, cursor := "pointer", color := omsheet.WHITE,
                    onClick --> { _ ⇒
                      selectedEntry.set(
                        if (isSelected) None
                        else Some(entry)
                      )
                    })
                ),
                div(
                  colBS(2),
                  downloadButton(entry, () ⇒ {
                    exists(manager.current.now ++ entry.name, entry)
                  })),
                div(colBS(7), (paddingTop := "7"),
                  label(entry.name, badge_primary, omsheet.tableTag)
                ),
                div(
                  if (isSelected)
                    omsheet.docEntry else emptyMod,
                  sEntry.map { se ⇒
                    if (isSelected) div(cls := "mdRendering", paddingTop := "40", se.readme, colSpan := 12)
                    else div()
                  }.getOrElse(div())
                )
              )
            )
          }
        }.getOrElse(Seq())
    }
  )

  def exists(sp: SafePath, entry: MarketIndexEntry) =
    Post()[Api].exists(sp).call().foreach { b ⇒
      if (b) overwriteAlert.set(Some(entry))
      else download(entry)
    }

  def download(entry: MarketIndexEntry) = {
    val path = manager.current.now ++ entry.name
    downloading.set(downloading.now.updatedFirst(_._1 == entry, (entry, Var(Processing()))))
    Post()[Api].getMarketEntry(entry, path).call().foreach { d ⇒
      downloading.update(d ⇒ d.updatedFirst(_._1 == entry, (entry, Var(Processed()))))
      downloading.now.headOption.foreach(_ ⇒ modalDialog.hide)
      panels.treeNodePanel.refreshAndDraw
    }
  }

  def downloadButton(entry: MarketIndexEntry, todo: () ⇒ Unit = () ⇒ {}) = div()

  //    downloading.signal.map {
  //    _.find {
  //      _._1 == entry
  //    }.map {
  //      case (e, state: Var[ProcessState]) ⇒
  //        state.withTransferWaiter { _ ⇒
  //          if (selectedEntry.now == Some(e)) glyphSpan(Seq(btn_danger, glyph_download_alt), onClick --> { _ ⇒ todo() }).amend("Download") else div()
  //        }
  //      case _ ⇒ div()
  //    }.getOrElse(div())
  //}

  def deleteFile(sp: SafePath, marketIndexEntry: MarketIndexEntry) =
    Post()[Api].deleteFile(sp, ServerFileSystemContext.project).call().foreach { d ⇒
      download(marketIndexEntry)
    }

  val marketHeader = span(b("Market place"))

  val marketBody = div(
    tagFilter.tag,
    marketTable
  )

  val marketFooter = closeButton("Close", () ⇒ modalDialog.hide).amend(btn_secondary)

  lazy val modalDialog: ModalDialog = ModalDialog(
    marketHeader,
    marketBody,
    marketFooter,
    omsheet.panelWidth(92),
    onopen = () ⇒ {
      marketIndex.now match {
        case None ⇒ Post()[Api].marketIndex.call().foreach { m ⇒
          marketIndex.set(Some(m))
        }
        case _ ⇒
      }
    },
    onclose = () ⇒ {}
  )

}
