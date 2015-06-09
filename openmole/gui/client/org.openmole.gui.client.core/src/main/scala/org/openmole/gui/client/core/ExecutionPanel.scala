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
import org.scalajs.dom.raw.HTMLDivElement
import org.scalajs.jquery
import scala.scalajs.js.Date
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.Expander
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.js.timers._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.ext.data.{ Error ⇒ ExecError }
import org.openmole.gui.ext.data._
import bs._
import rx._

class ExecutionPanel extends ModalPanel {
  val modalID = "executionsPanelID"

  val staticExecutionInfos: Var[Seq[(ExecutionId, StaticExecutionInfo)]] = Var(Seq())
  val executionInfos: Var[Seq[(ExecutionId, ExecutionInfo)]] = Var(Seq())
  val intervalHandler: Var[Option[SetIntervalHandle]] = Var(None)

  def allExecutionStates = {
    OMPost[Api].allExecutionStates.call().foreach { c ⇒
      executionInfos() = c
    }
    if (executionInfos().map {
      _._1
    }.toSet != staticExecutionInfos().map {
      _._1
    }.toSet) {
      OMPost[Api].allSaticInfos.call().foreach { i ⇒
        println("compute statics")
        staticExecutionInfos() = i
      }
    }
  }

  def onOpen = () ⇒ {
    allExecutionStates
    intervalHandler() = Some(setInterval(1000) {
      allExecutionStates
      if (executionInfos().filter {
        _._2 match {
          case r: Running ⇒ true
          case _          ⇒ false
        }
      }.isEmpty) onClose()
    })
  }

  def onClose = () ⇒ {
    intervalHandler().map {
      clearInterval
    }
  }

  case class ExecutionDetails(ratio: String, running: Long, error: Option[ExecError] = None)

  lazy val executionTable = {
    val expander = new Expander

    bs.table(striped)(
      thead,
      Rx {
        tbody({
          for ((id, executionInfo) ← executionInfos()) yield {
            val staticInfo = staticExecutionInfos().filter {
              _._1 == id
            }.headOption.getOrElse((id, StaticExecutionInfo()))._2
            val startDate = new Date(staticInfo.startDate).toLocaleDateString
            val duration = new Date(executionInfo.duration).toLocaleTimeString
            val completed = executionInfo.completed

            val details = executionInfo match {
              case f: Failed   ⇒ ExecutionDetails("0", 0, Some(f.error))
              case f: Finished ⇒ ExecutionDetails("100", 0)
              case r: Running  ⇒ ExecutionDetails((100 * completed.toDouble / (completed + r.ready)).formatted("%.0f"), r.running)
              case c: Canceled ⇒ ExecutionDetails("0", 0)
              case u: Unknown  ⇒ ExecutionDetails("0", 0)
            }

            val scriptLink = expander.getLink(staticInfo.name, id.id, "script", tags.div(bs.textArea(20)(staticInfo.script)))

            Seq(bs.tr(row)(
              bs.td(col_md_2)(scriptLink),
              bs.td(col_md_1)(startDate),
              bs.td(col_md_2)(bs.glyph(bs.glyph_flash), " " + details.running),
              bs.td(col_md_2)(bs.glyph(bs.glyph_flag), " " + completed),
              bs.td(col_md_1)(details.ratio + "%"),
              bs.td(col_md_1)(duration),
              bs.td(col_md_1)(executionInfo.state)(`class` := executionInfo.state + "State"),
              bs.td(col_md_1)(bs.glyphSpan(glyph_remove, () ⇒ OMPost[Api].cancelExecution(id).call().foreach { r ⇒
                allExecutionStates
              })(`class` := "cancelExecution")),
              bs.td(col_md_1)(bs.glyphSpan(glyph_trash, () ⇒ OMPost[Api].removeExecution(id).call().foreach { r ⇒
                allExecutionStates
              })(`class` := "removeExecution"))
            ),
              if (expander.isExpanded(id.id)) bs.tr(row)(
                tags.td(colspan := 12)(expander.hiddenDiv()(id.id)())
              )
              else tags.div()
            )
          }
        }
        )
      }
    ).render
  }

  val closeButton = bs.button("Close", btn_test)(data("dismiss") := "modal", onclick := {
    () ⇒
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