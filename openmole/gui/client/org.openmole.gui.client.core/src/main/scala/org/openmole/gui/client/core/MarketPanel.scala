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
import org.openmole.gui.ext.tool.client.{ InputFilter, _ }

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.client.core.files.TreeNodePanel
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.client.core.CoreUtils._
import org.openmole.gui.ext.data._
import Waiter._
import autowire._
import rx._
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import org.openmole.gui.ext.api.Api

class MarketPanel {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  private val marketIndex: Var[Option[MarketIndex]] = Var(None)
  val tagFilter = InputFilter(pHolder = "Filter")
  val selectedEntry: Var[Option[MarketIndexEntry]] = Var(None)
  lazy val downloading: Var[Seq[(MarketIndexEntry, Var[_ <: ProcessState])]] = Var(marketIndex.now.map {
    _.entries.map {
      (_, Var(Processed()))
    }
  }.getOrElse(Seq()))
  val overwriteAlert: Var[Option[MarketIndexEntry]] = Var(None)

  lazy val marketTable = div(
    paddingTop := 20,
    Rx {
      tagFilter.nameFilter()
      marketIndex().map { mindex ⇒
        for {
          entry ← mindex.entries if tagFilter.exists(entry.tags :+ entry.name)
        } yield {
          val isSelected = Some(entry) == selectedEntry()
          Seq(
            div(omsheet.docEntry)(
              div(colMD(3) +++ (paddingTop := 7))(
                tags.a(entry.name, cursor := "pointer", omsheet.color(omsheet.WHITE), onclick := { () ⇒
                  selectedEntry() = {
                    if (isSelected) None
                    else Some(entry)
                  }
                })
              ),
              div(colMD(2))(downloadButton(entry, () ⇒ {
                exists(manager.current.now ++ entry.name, entry)
              })),
              div(colMD(7) +++ (paddingTop := 7))(
                entry.tags.map { e ⇒ tags.label(e)(label_primary +++ omsheet.tableTag) }
              ), tags.div(
                if (isSelected) omsheet.docEntry else emptyMod,
                selectedEntry().map { se ⇒
                  if (isSelected) div(ms("mdRendering"))(paddingTop := 40)(
                    RawFrag(entry.readme.getOrElse(""))
                  )(colspan := 12)
                  else tags.div()
                }
              )
            )
          )
        }.render
      }
    }
  )

  def exists(sp: SafePath, entry: MarketIndexEntry) =
    post()[Api].exists(sp).call().foreach { b ⇒
      if (b) overwriteAlert() = Some(entry)
      else download(entry)
    }

  def download(entry: MarketIndexEntry) = {
    val path = manager.current.now ++ entry.name
    downloading() = downloading.now.updatedFirst(_._1 == entry, (entry, Var(Processing())))
    post()[Api].getMarketEntry(entry, path).call().foreach { d ⇒
      downloading() = downloading.now.updatedFirst(_._1 == entry, (entry, Var(Processed())))
      downloading.now.headOption.foreach(_ ⇒ dialog.hide)
      TreeNodePanel.refreshAndDraw
    }
  }

  def downloadButton(entry: MarketIndexEntry, todo: () ⇒ Unit = () ⇒ {}) = downloading.map {
    _.find {
      _._1 == entry
    }.map {
      case (e, state: Var[ProcessState]) ⇒
        state.withTransferWaiter { _ ⇒
          if (selectedEntry.now == Some(e)) buttonIcon(" Download", btn_danger, glyph_download_alt, todo) else tags.div()
        }
      case _ ⇒ tags.div()
    }.getOrElse(tags.div())
  }

  def deleteFile(sp: SafePath, marketIndexEntry: MarketIndexEntry) =
    post()[Api].deleteFile(sp, ServerFileSystemContext.project).call().foreach { d ⇒
      download(marketIndexEntry)
    }

  val dialog = ModalDialog(
    omsheet.panelWidth(92),
    onopen = () ⇒ {
      marketIndex.now match {
        case None ⇒ post()[Api].marketIndex.call().foreach { m ⇒
          marketIndex() = Some(m)
        }
        case _ ⇒
      }
    }
  )

  dialog.header(
    tags.span(tags.b("Market place"))
  )

  dialog.body({
    Rx {
      overwriteAlert() match {
        case Some(e: MarketIndexEntry) ⇒
          AlertPanel.string(
            e.name + " already exists. Overwrite ? ",
            () ⇒ {
              overwriteAlert() = None
              deleteFile(manager.current() ++ e.name, e)
            }, () ⇒ {
              overwriteAlert() = None
            }, CenterPagePosition
          )
          tags.div
        case _ ⇒
      }
    }
    tags.div(
      tagFilter.tag,
      marketTable
    )
  })

  dialog.footer(ModalDialog.closeButton(dialog, btn_default, "Close"))

}
