package org.openmole.gui.client.core

/*
 * Copyright (C) 17/05/15 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared.Api
import org.scalajs.jquery
import scala.scalajs.js.Date
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.js.timers._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.ext.data._
import bs._
import rx._

class ExecutionPanel extends ModalPanel {
  val modalID = "executionsPanelID"

  val moleExecutionUIs: Var[Seq[(ExecutionId, ExecutionInfo)]] = Var(Seq())
  val intervalHandler: Var[Option[SetIntervalHandle]] = Var(None)

  def allExecutionStates = {
    OMPost[Api].allExecutionStates.call().foreach { c ⇒
      moleExecutionUIs() = c
    }
  }

  def onOpen = () ⇒ {
    println("OPPPEn ")
    intervalHandler() = Some(setInterval(1000) {
      allExecutionStates
    })
  }

  def onClose = () ⇒ {
    intervalHandler().map {
      clearInterval
    }
  }

  val executionTable = bs.table(striped)(
    thead,
    Rx {
      tbody({
        for ((id, info) ← moleExecutionUIs()) yield {
          val startDate = new Date(id.startDate).toLocaleDateString
          val duration = new Date(info.duration).toLocaleTimeString
          val completed = info.completed

          val (ratio, running) = info match {
            case f: Failed   ⇒ ("0", 0)
            case f: Finished ⇒ ("100", 0)
            case r: Running  ⇒ ((100 * completed.toDouble / (completed + r.ready)).formatted("%.0f"), r.running)
            case c: Canceled ⇒ ("0", 0)
            case u: Unknown  ⇒ ("0", 0)
          }

          bs.tr(row)(
            bs.td(col_md_2)(tags.a(id.name, cursor := "pointer", onclick := { () ⇒
              println("clicked")
            })),
            bs.td(col_md_1)(id.name),
            bs.td(col_md_1)(startDate),
            bs.td(col_md_2)(bs.glyph(bs.glyph_flash), " " + running),
            bs.td(col_md_2)(bs.glyph(bs.glyph_flag), " " + completed),
            bs.td(col_md_1)(ratio + "%"),
            bs.td(col_md_1)(duration),
            bs.td(col_md_1)(info.state),
            bs.td(col_md_1)(bs.glyphSpan(glyph_trash, () ⇒ OMPost[Api].cancelExecution(id).call().foreach { r ⇒
              allExecutionStates
            }
            ))
          )
        }
      }
      )
    }
  ).render

  val closeButton = bs.button("Close", btn_test)(data("dismiss") := "modal", onclick := { () ⇒
    println("Close")
  }
  )

  val dialog = modalDialog(modalID,
    headerDialog(
      tags.div("Executions"
      ),
      bodyDialog(`class` := "executionTable")(
        executionTable
      ),
      footerDialog(
        closeButton
      )
    )
  )

  jquery.jQuery(org.scalajs.dom.document).on("hide.bs.modal", "#" + modalID, () ⇒ {
    onClose()
  })
}