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

import org.openmole.gui.misc.js.BootstrapTags.ScrollableTextArea.BottomScroll
import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.HTMLDivElement
import org.scalajs.jquery
import scala.concurrent
import scala.concurrent.duration.Duration
import scala.scalajs.js.Date
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.Expander
import org.openmole.gui.misc.js.Expander._
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
import concurrent.duration._

class ExecutionPanel extends ModalPanel {
  val modalID = "executionsPanelID"

  val staticExecutionInfos: Var[Seq[(ExecutionId, StaticExecutionInfo)]] = Var(Seq())
  val executionInfos: Var[Seq[(ExecutionId, ExecutionInfo)]] = Var(Seq())
  val outputsInfos: Var[Seq[RunningOutputData]] = Var(Seq())
  val envErrorsInfos: Var[Seq[RunningEnvironmentData]] = Var(Seq())
  val intervalHandler: Var[Option[SetIntervalHandle]] = Var(None)
  val envAndOutIntervalHandler: Var[Option[SetIntervalHandle]] = Var(None)
  val expander = new Expander

  def allExecutionStates = {
    OMPost[Api].allExecutionStates.call().foreach { c ⇒
      executionInfos() = c
    }

    if (executionInfos().map {
      _._1
    }.toSet != staticExecutionInfos().map {
      _._1
    }.toSet) {
      OMPost[Api].allStaticInfos.call().foreach { i ⇒
        staticExecutionInfos() = i
      }
    }
  }

  def allEnvStatesAndOutputs = {
    OMPost[Api].runningErrorEnvironmentAndOutputData.call().foreach { r ⇒
      envErrorsInfos() = r._1
      outputsInfos() = r._2
    }
  }

  def atLeastOneRunning = executionInfos().filter {
    _._2 match {
      case r: Running ⇒ true
      case _          ⇒ false
    }
  }.isEmpty

  def onOpen = () ⇒ {
    allExecutionStates
    allEnvStatesAndOutputs
    intervalHandler() = Some(setInterval(1000) {
      allExecutionStates
      if (atLeastOneRunning) onClose()
    })

    envAndOutIntervalHandler() = Some(setInterval(7000) {
      allEnvStatesAndOutputs
      if (atLeastOneRunning) onClose()
    })
  }

  def onClose = () ⇒ {
    allEnvStatesAndOutputs
    intervalHandler().map {
      clearInterval
    }
    envAndOutIntervalHandler().map {
      clearInterval
    }
  }

  case class ExecutionDetails(ratio: String,
                              running: Long,
                              error: Option[ExecError] = None,
                              envStates: Seq[EnvironmentState] = Seq(),
                              outputs: String = "")

  val outputTextAreas: Var[Map[ExecutionId, BSTextArea]] = Var(Map())
  val envErrorTextAreas: Var[Map[ExecutionId, BSTextArea]] = Var(Map())

  lazy val executionTable = {
    bs.table(striped)(
      thead,
      Rx {
        tbody({
          for ((id, executionInfo) ← executionInfos()) yield {
            val staticInfo =
              staticExecutionInfos().find {
                _._1 == id
              }.get._2
            val startDate = s"${new Date(staticInfo.startDate).toLocaleDateString} : ${new Date(staticInfo.startDate).toLocaleTimeString}"

            val duration: Duration = (executionInfo.duration milliseconds)
            val h = (duration).toHours
            val m = ((duration) - (h hours)).toMinutes
            val s = (duration - (h hours) - (m minutes)).toSeconds

            val durationString = s"""${h.formatted("%d")}:${m.formatted("%02d")}:${s.formatted("%02d")}"""

            val completed = executionInfo.completed

            val details = executionInfo match {
              case f: Failed   ⇒ ExecutionDetails("0", 0, Some(f.error))
              case f: Finished ⇒ ExecutionDetails("100", 0, envStates = f.environmentStates)
              case r: Running  ⇒ ExecutionDetails((100 * completed.toDouble / (completed + r.ready)).formatted("%.0f"), r.running, envStates = r.environmentStates)
              case c: Canceled ⇒ ExecutionDetails("0", 0)
              case r: Ready    ⇒ ExecutionDetails("0", 0)
            }

            val scriptID: VisibleID = "script"
            val envID: VisibleID = "env"
            val errorID: VisibleID = "error"
            val outputStreamID: VisibleID = "outputStream"
            val envErrorID: VisibleID = "envError"

            val scriptLink = expander.getLink(staticInfo.path.name, id.id, scriptID)
            val envLink = expander.getGlyph(glyph_stats, "Env", id.id, envID)
            val stateLink = executionInfo match {
              case f: Failed ⇒ expander.getLink(executionInfo.state, id.id, errorID)
              case _         ⇒ tags.span(executionInfo.state)
            }
            val outputLink = expander.getGlyph(glyph_list, "", id.id, outputStreamID)

            val envErrorLink = expander.getLink("EnvErr", id.id, envErrorID)

            val hiddenMap = Map(
              scriptID -> tags.div(bs.textArea(20)(staticInfo.script)),
              envID -> {
                tags.div(
                  details.envStates.map { e ⇒
                    bs.table(striped)(`class` := "executionTable")(
                      thead,
                      tbody(
                        Seq(bs.tr(row)(
                          bs.td(col_md_2)(e.taskName),
                          bs.td(col_md_3)("Submitted: " + e.submitted),
                          bs.td(col_md_2)(bs.glyph(bs.glyph_flash), " " + e.running),
                          bs.td(col_md_2)(bs.glyph(bs.glyph_flag), " " + e.done),
                          bs.td(col_md_3)("Failed: " + e.failed)
                        )
                        )
                      )
                    )
                  }
                )
              },
              errorID -> tags.div(bs.textArea(20)(new String(details.error.map {
                _.stackTrace
              }.getOrElse("")))),
              outputStreamID -> {

                val outputs = outputsInfos().filter {
                  _.id == id
                }.map {
                  _.output
                }.mkString("\n")

                val tArea = outputTextAreas().get(id) match {
                  case Some(t: BSTextArea) ⇒
                    t.content = outputs
                    t
                  case None ⇒
                    val newTA = new BSTextArea(20, outputs, BottomScroll())
                    outputTextAreas() = outputTextAreas() + (id -> newTA)
                    newTA
                }
                tArea.get
              },
              envErrorID -> {

                val outputs = envErrorsInfos().filter {
                  _.id == id
                }.map {
                  _.errors.map {
                    _.errorMessage
                  }.mkString("\n")
                }.mkString("\n")

                val tArea = envErrorTextAreas().get(id) match {
                  case Some(t: BSTextArea) ⇒
                    t.content = outputs
                    t
                  case None ⇒
                    val newTA = new BSTextArea(20, outputs, BottomScroll())
                    envErrorTextAreas() = envErrorTextAreas() + (id -> newTA)
                    newTA
                }
                tArea.get
              }
            )

            Seq(bs.tr(row)(
              bs.td(col_md_2)(visibleClass(id.id, scriptID))(scriptLink),
              bs.td(col_md_3 + "small")(startDate),
              bs.td(col_md_1)(bs.glyph(bs.glyph_flash), " " + details.running),
              bs.td(col_md_1)(bs.glyph(bs.glyph_flag), " " + completed),
              bs.td(col_md_1)(details.ratio + "%"),
              bs.td(col_md_1)(durationString),
              bs.td(col_md_1)(stateLink)(`class` := executionInfo.state + "State"),
              bs.td(col_md_1)(visibleClass(id.id, envID))(envLink),
              bs.td(col_md_1)(visibleClass(id.id, envErrorID))(envErrorLink),
              bs.td(col_md_1)(visibleClass(id.id, outputStreamID))(outputLink),
              bs.td(col_md_1)(bs.glyphSpan(glyph_remove, () ⇒ OMPost[Api].cancelExecution(id).call().foreach { r ⇒
                allExecutionStates
              })(`class` := "cancelExecution")),
              bs.td(col_md_1)(bs.glyphSpan(glyph_trash, () ⇒ OMPost[Api].removeExecution(id).call().foreach { r ⇒
                allExecutionStates
              })(`class` := "removeExecution"))
            ), bs.tr(row)(
              expander.getVisible(id.id) match {
                case Some(v: VisibleID) ⇒ tags.td(colspan := 12)(hiddenMap(v))
                case _                  ⇒ tags.div()
              }
            )
            )
          }
        }
        )
      }
    ).render
  }

  def visibleClass(expandID: ExpandID, visibleID: VisibleID): Modifier = `class` := {
    if (expander.isVisible(expandID, visibleID)) "executionVisible" else ""
  }

  val closeButton = bs.button("Close", btn_primary)(data("dismiss") := "modal", onclick := {
    () ⇒
  }
  )

  val dialog = modalDialog(modalID,
    headerDialog(
      tags.div(tags.b("Executions")),
      bodyDialog(`class` := "executionTable")(
        executionTable),
      footerDialog(
        closeButton
      )
    )
  )

  jquery.jQuery(org.scalajs.dom.document).on("hide.bs.modal", "#" + modalID, () ⇒ {
    onClose()
  })
}