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

import org.openmole.gui.client.core.EnvironmentErrorPanel.SelectableLevel
import org.openmole.gui.misc.js.BootstrapTags.ScrollableTextArea.BottomScroll
import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared.Api
import scala.concurrent.duration.Duration
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs, Select, Expander }
import org.openmole.gui.misc.js.Expander._
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

  case class PanelInfo(
    executionInfos: Seq[(ExecutionId, StaticExecutionInfo, ExecutionInfo)],
    outputsInfos: Seq[RunningOutputData],
    envErrorsInfos: Seq[RunningEnvironmentData])

  val panelInfo = Var(PanelInfo(Seq(), Seq(), Seq()))

  val intervalHandler: Var[Option[SetIntervalHandle]] = Var(None)
  val expander = new Expander

  def updatePanelInfo = {
    OMPost[Api].allStates.call().foreach { executionInfos ⇒
      //FIXME select the number of lines from the panel
      OMPost[Api].runningErrorEnvironmentAndOutputData(lines = nbOutLineInput.value.toInt, errorLevelSelector.content().map {
        _.level
      }.getOrElse(ErrorLevel())).call().foreach { err ⇒
        panelInfo() = PanelInfo(executionInfos, err._2, err._1)
        doScrolls
      }
    }
  }

  def onOpen = () ⇒ {
    updatePanelInfo
    intervalHandler() = Some(setInterval(5000) {
      updatePanelInfo
    })
  }

  def onClose = () ⇒ {
    intervalHandler().map {
      clearInterval
    }
  }

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

  case class ExecutionDetails(ratio: String,
                              running: Long,
                              error: Option[ExecError] = None,
                              envStates: Seq[EnvironmentState] = Seq(),
                              outputs: String = "")

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
      panelMap() = panelMap() + (id -> toBeAdded)
      toBeAdded
    }
  }

  val envLevel: Var[ErrorStateLevel] = Var(ErrorLevel())

  val errorLevelSelector: Select[SelectableLevel] = Select("errorLevel", Seq(ErrorLevel(), DebugLevel()).map { level ⇒
    (SelectableLevel(level, level.name), emptyCK)
  }, Some(envLevel()), btn_primary, () ⇒ errorLevelSelector.content().map { l ⇒ envLevel() = l.level })

  val nbOutLineInput = bs.input("500")(width := "60px").render

  def ratio(completed: Long, running: Long, ready: Long) = s"${completed} / ${completed + running + ready}"

  val envErrorVisible: Var[Seq[EnvironmentId]] = Var(Seq())

  lazy val executionTable = {
    bs.table(striped)(
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
              scriptID -> staticPanel(id, scriptTextAreas,
                () ⇒ {
                  scrollableText(staticInfo.script)
                }
              ).view,
              envID -> {
                tags.div(
                  details.envStates.map { e ⇒
                    bs.table(striped)(`class` := "executionTable")(
                      thead,
                      tbody(
                        Seq(bs.tr(row)(
                          bs.td(col_md_2)(e.taskName),
                          bs.td(col_md_2)(bs.glyph(bs.glyph_upload), s" ${e.networkActivity.uploadingFiles} ${displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize)}"),
                          bs.td(col_md_2)(bs.glyph(bs.glyph_download), s" ${e.networkActivity.downloadingFiles} ${displaySize(e.networkActivity.downloadedSize, e.networkActivity.readableDownloadedSize)}"),
                          bs.td(col_md_1)(bs.glyph(bs.glyph_road), " " + e.submitted),
                          bs.td(col_md_1)(bs.glyph(bs.glyph_flash), " " + e.running),
                          bs.td(col_md_2)(bs.glyph(bs.glyph_flag), " " + e.done),
                          bs.td(col_md_1)(bs.glyph(bs.glyph_fire), " " + e.failed),
                          bs.td(col_md_1)(bs.span({
                            "blue" + {
                              if (envErrorVisible().contains(e.envId)) " executionVisible" else ""
                            }
                          })(cursor := "pointer", onclick := {
                            () ⇒
                              {
                                if (envErrorVisible().contains(e.envId)) envErrorVisible() = envErrorVisible().filterNot {
                                  _ == e.envId
                                }
                                else envErrorVisible() = envErrorVisible() :+ e.envId
                              }
                          }
                          )("details")
                          )),
                          bs.tr(row)(
                            bs.td(col_md_12)(
                              `class` := {
                                if (envErrorVisible().contains(e.envId)) "displayNone" else ""
                              },
                              colspan := 12,
                              staticPanel(e.envId, envErrorPanels,
                                () ⇒ new EnvironmentErrorPanel,
                                (ep: EnvironmentErrorPanel) ⇒ {
                                  ep.setErrors(panelInfo().envErrorsInfos.flatMap {
                                    _.errors
                                  }.filter {
                                    _.id == e.envId
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
              errorID -> staticPanel(id, errorTextAreas,
                () ⇒ scrollableText(),
                (sT: ScrollableText) ⇒ sT.setContent(new String(details.error.map {
                  _.stackTrace
                }.getOrElse("")))
              ).view,
              outputStreamID -> staticPanel(
                id,
                outputTextAreas,
                () ⇒ {
                  scrollableText("", BottomScroll())
                },
                (sT: ScrollableText) ⇒ sT.setContent(
                  panelInfo().outputsInfos.filter {
                    _.id == id
                  }.map {
                    _.output
                  }.mkString("\n"))
              ).view
            )

            Seq(bs.tr(row)(
              bs.td(col_md_2)(visibleClass(id.id, scriptID))(scriptLink),
              bs.td(col_md_2 + "small")(Utils.longToDate(staticInfo.startDate)),
              bs.td(col_md_1)(bs.glyph(bs.glyph_flash), " " + details.running),
              bs.td(col_md_2)(bs.glyph(bs.glyph_flag), " " + details.ratio),
              bs.td(col_md_1)(durationString),
              bs.td(col_md_1)(stateLink)(`class` := executionInfo.state + "State"),
              bs.td(col_md_1)(visibleClass(id.id, envID))(envLink),
              bs.td(col_md_1)(visibleClass(id.id, outputStreamID))(outputLink),
              bs.td(col_md_1)(bs.glyphSpan(glyph_remove, () ⇒ OMPost[Api].cancelExecution(id).call().foreach { r ⇒
                updatePanelInfo
              })(`class` := "cancelExecution")),
              bs.td(col_md_1)(bs.glyphSpan(glyph_trash, () ⇒ OMPost[Api].removeExecution(id).call().foreach { r ⇒
                updatePanelInfo
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

  def displaySize(size: Long, readable: String) =
    if (size == 0L) ""
    else s"($readable)"

  def visibleClass(expandID: ExpandID, visibleID: VisibleID): Modifier = `class` := {
    if (expander.isVisible(expandID, visibleID)) "executionVisible" else ""
  }

  val closeButton = bs.button("Close", btn_primary)(data("dismiss") := "modal", onclick := {
    () ⇒ close
  }
  )

  val dialog = modalDialog(modalID,
    headerDialog(
      tags.div(
        tags.b("Executions"),
        tags.div(
          bs.div("width40")(
            tags.label("Output history "),
            nbOutLineInput
          ),
          bs.div("width40")(
            tags.label("Environment error level "),
            errorLevelSelector.selector
          )
        )
      )),
    bodyDialog(`class` := "executionTable")(
      executionTable),
    footerDialog(
      closeButton
    )
  )

}