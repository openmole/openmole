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

import java.util.concurrent.atomic.AtomicBoolean

import org.openmole.gui.client.core.EnvironmentErrorPanel.SelectableLevel
import fr.iscpif.scaladget.api.BootstrapTags.ScrollableTextArea.BottomScroll
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared.Api
import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success }
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{ _ }
import org.openmole.gui.misc.js.Expander._
import scalatags.JsDom._
import org.openmole.gui.misc.js.Tooltip._
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.js.timers._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet }
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import sheet._
import autowire._
import org.openmole.gui.ext.data.{ Error ⇒ ExecError }
import org.openmole.gui.ext.data._
import bs._
import rx._
import concurrent.duration._
import style._

class ExecutionPanel extends ModalPanel {
  lazy val modalID = "executionsPanelID"

  case class PanelInfo(
    executionInfos: Seq[(ExecutionId, StaticExecutionInfo, ExecutionInfo)],
    outputsInfos:   Seq[RunningOutputData],
    envErrorsInfos: Seq[RunningEnvironmentData]
  )

  val panelInfo = Var(PanelInfo(Seq(), Seq(), Seq()))
  val expander = new Expander

  val updating = new AtomicBoolean(false)

  def updatePanelInfo: Unit = {
    def delay = {
      updating.set(false)
      setTimeout(5000) {
        if (isVisible) updatePanelInfo
      }
    }

    if (updating.compareAndSet(false, true)) {
      OMPost[Api].allStates.call().andThen {
        case Success(executionInfos) ⇒
          OMPost[Api].runningErrorEnvironmentAndOutputData(
            lines = nbOutLineInput.value.toInt,
            errorLevelSelector.content().map {
              _.level
            }.getOrElse(ErrorLevel())
          ).call().andThen {
              case Success(err) ⇒
                panelInfo() = PanelInfo(executionInfos, err._2, err._1)
                doScrolls
                delay
              case Failure(_) ⇒
                delay
            }
        case Failure(_) ⇒ delay
      }
    }
  }

  def onOpen() = {
    setTimeout(0) {
      updatePanelInfo
    }
  }

  def onClose() = {}

  def doScrolls = {
    Seq(outputTextAreas(), scriptTextAreas(), errorTextAreas()).map {
      _.values.foreach {
        _.doScroll
      }
    }
    envErrorPanels().values.foreach {
      _.scrollable.doScroll
    }
  }

  case class ExecutionDetails(
    ratio:     String,
    running:   Long,
    error:     Option[ExecError]     = None,
    envStates: Seq[EnvironmentState] = Seq(),
    outputs:   String                = ""
  )

  val scriptTextAreas: Var[Map[ExecutionId, ScrollableText]] = Var(Map())
  val errorTextAreas: Var[Map[ExecutionId, ScrollableText]] = Var(Map())
  val outputTextAreas: Var[Map[ExecutionId, ScrollableText]] = Var(Map())
  val envErrorPanels: Var[Map[EnvironmentId, EnvironmentErrorPanel]] = Var(Map())

  def staticPanel[T, I <: ID](id: I, panelMap: Var[Map[I, T]], builder: () ⇒ T, appender: T ⇒ Unit = (t: T) ⇒ {}): T = {
    if (panelMap().isDefinedAt(id)) {
      val t = panelMap()(id)
      appender(t)
      t
    }
    else {
      val toBeAdded = builder()
      panelMap() = panelMap() + (id → toBeAdded)
      toBeAdded
    }
  }

  val envLevel: Var[ErrorStateLevel] = Var(ErrorLevel())

  val errorLevelSelector: Select[SelectableLevel] = Select( /*"errorLevel",*/ Seq(ErrorLevel(), DebugLevel()).map { level ⇒
    (SelectableLevel(level, level.name), emptyMod)
  }, Some(envLevel()), btn_primary, () ⇒ errorLevelSelector.content().map { l ⇒ envLevel() = l.level })

  val nbOutLineInput = bs.input("500")(colMD(1) +++ (width := 60)).render

  def ratio(completed: Long, running: Long, ready: Long) = s"${completed} / ${completed + running + ready}"

  val envErrorVisible: Var[Seq[EnvironmentId]] = Var(Seq())

  lazy val executionTable = {
    tags.table(sheet.table +++ striped +++ ms("executionTable"))(
      thead,
      Rx {
        tbody({
          for {
            (id, staticInfo, executionInfo) ← panelInfo().executionInfos.sortBy(_._2.startDate).reverse
          } yield {

            val duration: Duration = (executionInfo.duration milliseconds)
            val h = (duration).toHours
            val m = ((duration) - (h hours)).toMinutes
            val s = (duration - (h hours) - (m minutes)).toSeconds

            val durationString = s"""${h.formatted("%d")}:${m.formatted("%02d")}:${s.formatted("%02d")}"""

            val completed = executionInfo.completed

            val details = executionInfo match {
              case f: Failed   ⇒ ExecutionDetails("0", 0, Some(f.error))
              case f: Finished ⇒ ExecutionDetails(ratio(f.completed, f.running, f.ready), f.running, envStates = f.environmentStates)
              case r: Running  ⇒ ExecutionDetails(ratio(r.completed, r.running, r.ready), r.running, envStates = r.environmentStates)
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
            val outputLink = expander.getGlyph(glyph_list, "", id.id, outputStreamID, () ⇒ doScrolls)

            lazy val hiddenMap: Map[VisibleID, Modifier] = Map(
              scriptID → staticPanel(id, scriptTextAreas,
                () ⇒ scrollableText(staticInfo.script)).view,
              envID → {
                div(
                  details.envStates.map { e ⇒
                    tags.table(ms("executionTable"))(
                      thead,
                      tbody(
                        Seq(
                          tr(row)(
                            td(colMD(3))(tags.span(e.taskName).tooltip("Environment name")),
                            td(colMD(2))(tags.span(glyph_upload)(s" ${e.networkActivity.uploadingFiles} ${displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize)}").tooltip("Uploaded")),
                            td(colMD(2))(tags.span(glyph_download)(s" ${e.networkActivity.downloadingFiles} ${displaySize(e.networkActivity.downloadedSize, e.networkActivity.readableDownloadedSize)}").tooltip("Downloaded")),
                            td(colMD(2))(tags.span(glyph_road +++ sheet.paddingBottom(7))(" " + e.submitted).tooltip("Submitted jobs")),
                            td(colMD(1))(tags.span(glyph_flash +++ sheet.paddingBottom(7))(" " + e.running).tooltip("Running jobs")),
                            td(colMD(1))(tags.span(glyph_flag +++ sheet.paddingBottom(7))(" " + e.done).tooltip("Completed jobs")),
                            td(colMD(1))(tags.span(glyph_fire +++ sheet.paddingBottom(7))(" " + e.failed).tooltip("Failed jobs")),
                            td(colMD(3))(tags.span(omsheet.color("#3086b5") +++ ((envErrorVisible().contains(e.envId)), ms(" executionVisible"), emptyMod))(
                              sheet.pointer, onclick := { () ⇒
                              if (envErrorVisible().contains(e.envId)) envErrorVisible() = envErrorVisible().filterNot {
                                _ == e.envId
                              }
                              else envErrorVisible() = envErrorVisible() :+ e.envId
                            }
                            )("details"))
                          ),
                          tr(row)(
                            td(colMD(12) +++ (!envErrorVisible().contains(e.envId), omsheet.displayOff, emptyMod))(
                              colspan := 12,
                              staticPanel(e.envId, envErrorPanels,
                                () ⇒ new EnvironmentErrorPanel,
                                (ep: EnvironmentErrorPanel) ⇒ {
                                  ep.setErrors(panelInfo().envErrorsInfos.flatMap {
                                    _.errors
                                  }.filter {
                                    _._1.environmentId == e.envId
                                  })
                                }).view
                            )
                          )
                        )
                      )
                    )
                  }
                )
              },
              errorID →
                div(
                  omsheet.monospace,
                  staticPanel(
                  id,
                  errorTextAreas,
                  () ⇒ scrollableText(),
                  (sT: ScrollableText) ⇒ sT.setContent(new String(details.error.map {
                    _.stackTrace
                  }.getOrElse("")))
                ).view
                ),
              outputStreamID → staticPanel(
                id,
                outputTextAreas,
                () ⇒ scrollableText("", BottomScroll),
                (sT: ScrollableText) ⇒ sT.setContent(
                  panelInfo().outputsInfos.filter {
                    _.id == id
                  }.map {
                    _.output
                  }.mkString("\n")
                )
              ).view
            )

            Seq(
              tr(row)(
                td(colMD(2))(visibleClass(id.id, scriptID))(scriptLink.tooltip("Show script")),
                td(colMD(2) +++ "small")(div(Utils.longToDate(staticInfo.startDate)).tooltip("Start time")),
                td(colMD(2))(tags.span(glyph_flash)(" " + details.running).tooltip("Running jobs")),
                td(colMD(2))(tags.span(glyph_flag)(" " + details.ratio).tooltip("Jobs progression")),
                td(colMD(1))(div(durationString).tooltip("Execution duration")),
                td(colMD(1))(stateLink.tooltip("Execution state"))(ms(executionInfo.state + "State vert-align")),
                td(colMD(1))(visibleClass(id.id, envID))(envLink),
                td(colMD(1))(visibleClass(id.id, outputStreamID))(outputLink.tooltip("Execution outputs")),
                td(colMD(1))(tags.span(glyph_remove +++ ms("removeExecution"), onclick := { () ⇒
                  OMPost[Api].cancelExecution(id).call().foreach { r ⇒
                    updatePanelInfo
                  }
                }).tooltip("Cancel execution", level = WarningTooltipLevel())),
                td(colMD(1))(tags.span(glyph_trash +++ ms("removeExecution"), onclick := { () ⇒
                  OMPost[Api].removeExecution(id).call().foreach { r ⇒
                    updatePanelInfo
                  }
                }).tooltip("Remove execution", level = WarningTooltipLevel()))
              ),
              tr(row)(
                expander.getVisible(id.id) match {
                  case Some(v: VisibleID) ⇒ td(colspan := 12)(hiddenMap(v))
                  case _                  ⇒ div()
                }
              )
            )
          }
        })
      }
    ).render
  }

  def displaySize(size: Long, readable: String) =
    if (size == 0L) ""
    else s"($readable)"

  def visibleClass(expandID: ExpandID, visibleID: VisibleID): Modifier = ms(
    "vert-align " + {
      if (expander.isVisible(expandID, visibleID)) "executionVisible" else ""
    }
  )

  val dialog = bs.modalDialog(
    modalID,
    headerDialog(
      div(height := 55)(
        b("Executions"),
        div(
          div(omsheet.executionHeader)(
            tags.label(colMD(4) +++ omsheet.execOutput, "Output history"),
            nbOutLineInput
          ),
          div(omsheet.executionHeader)(
            tags.label(colMD(6) +++ omsheet.execLevel, "Environment error level "),
            div(colMD(1))(errorLevelSelector.selector)
          )
        )
      )
    ),
    bodyDialog(ms("executionTable"))(
      executionTable
    ),
    footerDialog(
      closeButton
    )
  )

}