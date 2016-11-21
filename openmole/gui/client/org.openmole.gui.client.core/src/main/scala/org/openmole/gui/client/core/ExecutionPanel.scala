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

import fr.iscpif.scaladget.api.BootstrapTags.ScrollableTextArea.BottomScroll
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet, Utils }
import org.openmole.gui.shared.Api
import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success }
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{ _ }
import org.openmole.gui.misc.js.Expander._
import scalatags.JsDom._
import org.openmole.gui.misc.js.JsRxTags._
import scala.scalajs.js.timers._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import sheet._
import autowire._
import org.openmole.gui.ext.data.{ Error ⇒ ExecError }
import org.openmole.gui.ext.data._
import bs._
import rx._
import concurrent.duration._

class ExecutionPanel {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  case class PanelInfo(
    executionInfos: Seq[(ExecutionId, ExecutionInfo)],
    outputsInfos:   Seq[RunningOutputData]
  )

  val execInfo = Var(PanelInfo(Seq(), Seq()))
  val staticInfo: Var[Map[ExecutionId, StaticExecutionInfo]] = Var(Map())
  var envError: Var[Map[EnvironmentId, EnvironmentErrorData]] = Var(Map())
  val expanders: Var[Map[ExpandID, Expander]] = Var(Map())

  val updating = new AtomicBoolean(false)

  def expander[T](id: ExpandID, todo: Expander ⇒ T) = expanders.map { ex ⇒ todo(ex(id)) }

  def expanderIfVisible[T](id: ExpandID, columnID: ColumnID, todo: Expander ⇒ T, otherwise: T) = {
    expanders.map { ex ⇒
      if (ex(id).isVisible(columnID)) todo(ex(id))
      else otherwise
    }
  }

  def closeAllExpanders = expanders.now.values.map {
    _.close
  }

  def updateExecutionInfo: Unit = {
    def delay = {
      updating.set(false)
      setTimeout(5000) {
        if (dialog.isVisible) updateExecutionInfo
      }
    }

    if (updating.compareAndSet(false, true)) {
      OMPost[Api].allStates(outputHistory.value.toInt).call().andThen {
        case Success((executionInfos, runningOutputData)) ⇒
          execInfo() = PanelInfo(executionInfos, runningOutputData)
          doScrolls
          delay
        case Failure(_) ⇒ delay
      }
    }
  }

  def updateStaticInfos = OMPost[Api].staticInfos.call().foreach { s ⇒
    staticInfo() = s.toMap
    setTimeout(0) {
      updateExecutionInfo
    }
  }

  def onOpen() = {
    closeAllExpanders
    updateStaticInfos
  }

  def onClose() = {}

  def doScrolls = {
    Seq(outputTextAreas.now, scriptTextAreas.now, errorTextAreas.now).map {
      _.values.foreach {
        _.doScroll
      }
    }
    envErrorPanels.now.values.foreach {
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
    if (panelMap.now.isDefinedAt(id)) {
      val t = panelMap.now(id)
      appender(t)
      t
    }
    else {
      val toBeAdded = builder()
      panelMap() = panelMap.now + (id → toBeAdded)
      toBeAdded
    }
  }

  val envLevel: Var[ErrorStateLevel] = Var(ErrorLevel())

  val outputHistory = bs.input("500")(placeholder := "# outputs").render
  val envErrorHistory = bs.input("500")(placeholder := "# environment errors").render

  def ratio(completed: Long, running: Long, ready: Long) = s"${completed} / ${completed + running + ready}"

  val envErrorVisible: Var[Seq[EnvironmentId]] = Var(Seq())

  def glyphAndText(mod: ModifierSeq, text: String) = tags.span(
    tags.span(mod),
    s" $text"
  )

  lazy val executionTable = {
    val scriptID: ColumnID = "script"
    val envID: ColumnID = "env"
    val errorID: ColumnID = "error"
    val outputStreamID: ColumnID = "outputStream"
    tags.table(sheet.table)(
      thead,
      Rx {
        tbody({
          for {
            (id, executionInfo) ← execInfo().executionInfos.sortBy { case (execId, _) ⇒ staticInfo.now(execId).startDate }.reverse
          } yield {
            if (!expanders().keys.exists(ex ⇒ ex == id.id)) {
              expanders() = expanders() ++ Map(id.id → new Expander(id.id))
            }
            val theExpanders = expanders()
            val duration: Duration = (executionInfo.duration milliseconds)
            val h = (duration).toHours
            val m = ((duration) - (h hours)).toMinutes
            val s = (duration - (h hours) - (m minutes)).toSeconds

            val durationString = s"""${h.formatted("%d")}:${m.formatted("%02d")}:${s.formatted("%02d")}"""

            val completed = executionInfo.completed

            val details = executionInfo match {
              case f: Failed   ⇒ ExecutionDetails("0", 0, Some(f.error), f.environmentStates)
              case f: Finished ⇒ ExecutionDetails(ratio(f.completed, f.running, f.ready), f.running, envStates = f.environmentStates)
              case r: Running  ⇒ ExecutionDetails(ratio(r.completed, r.running, r.ready), r.running, envStates = r.environmentStates)
              case c: Canceled ⇒ ExecutionDetails("0", 0, envStates = c.environmentStates)
              case r: Ready    ⇒ ExecutionDetails("0", 0, envStates = r.environmentStates)
            }

            val scriptLink = expander(id.id, ex ⇒ ex.getLink(staticInfo.now(id).path.name, scriptID))
            val envLink = expander(id.id, ex ⇒ ex.getGlyph(glyph_stats, "Env", envID))
            val stateLink = expander(id.id, ex ⇒
              executionInfo match {
                case f: Failed ⇒ ex.getLink(executionInfo.state, errorID).render
                case _         ⇒ tags.span(executionInfo.state).render
              })
            val outputLink = expander(id.id, ex ⇒ ex.getGlyph(glyph_list, "", outputStreamID, () ⇒ doScrolls))

            val hiddenMap: Map[ColumnID, Modifier] = Map(
              scriptID → staticPanel(id, scriptTextAreas,
                () ⇒ scrollableText(staticInfo.now(id).script)).view,
              envID → {
                details.envStates.map { e ⇒
                  tags.table(sheet.table)(
                    thead,
                    tbody(
                      Seq(
                        tr(row +++ (fontSize := 14))(
                          td(colMD(1))(tags.span(e.taskName)),
                          td(colMD(2))(tags.span(CoreUtils.approximatedYearMonthDay(e.executionActivity.executionTime))),
                          td(colMD(2))(glyphAndText(glyph_upload, s" ${e.networkActivity.uploadingFiles} ${displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize)}")),
                          td(colMD(2))(glyphAndText(glyph_download, s" ${e.networkActivity.downloadingFiles} ${displaySize(e.networkActivity.downloadedSize, e.networkActivity.readableDownloadedSize)}")),
                          td(colMD(1))(glyphAndText(glyph_road +++ sheet.paddingBottom(7), e.submitted.toString)),
                          td(colMD(1))(glyphAndText(glyph_flash +++ sheet.paddingBottom(7), e.running.toString)),
                          td(colMD(1))(glyphAndText(glyph_flag +++ sheet.paddingBottom(7), e.done.toString)),
                          td(colMD(1))(glyphAndText(glyph_fire +++ sheet.paddingBottom(7), e.failed.toString)),
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
                          {
                            td(colMD(12) +++ (!envErrorVisible().contains(e.envId), omsheet.displayOff, emptyMod))(
                              colspan := 12,
                              bs.buttonGroup(omsheet.centerElement)(
                                bs.button("Update", () ⇒ updateEnvErrors(e.envId, false)),
                                bs.button("Reset", () ⇒ updateEnvErrors(e.envId, true))
                              ),
                              staticPanel(e.envId, envErrorPanels,
                                () ⇒ new EnvironmentErrorPanel,
                                (ep: EnvironmentErrorPanel) ⇒
                                  ep.setErrors(envError().getOrElse(e.envId, EnvironmentErrorData.empty))).view
                            )
                          }
                        )
                      )
                    )
                  )
                }
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
                  execInfo().outputsInfos.filter {
                    _.id == id
                  }.map {
                    _.output
                  }.mkString("\n")
                )
              ).view
            )

            Seq(
              tr(row +++ omsheet.executionTable, colspan := 12)(
                td(colMD(2), pointer)(visibleClass(id.id, scriptID, scriptLink)),
                td(colMD(2))(div(Utils.longToDate(staticInfo.now(id).startDate))),
                td(colMD(2))(glyphAndText(glyph_flash, details.running.toString)),
                td(colMD(2))(glyphAndText(glyph_flag, details.ratio.toString)),
                td(colMD(1))(div(durationString)),
                td(colMD(1))(visibleClass(id.id, errorID, stateLink, omsheet.executionState(executionInfo.state))),
                td(colMD(1), pointer)(visibleClass(id.id, envID, envLink)),
                td(colMD(1), pointer)(visibleClass(id.id, outputStreamID, outputLink)),
                td(colMD(1))(tags.span(glyph_remove +++ ms("removeExecution"), onclick := { () ⇒
                  cancelExecution(id)
                })),
                td(colMD(1))(tags.span(glyph_trash +++ ms("removeExecution"), onclick := { () ⇒
                  removeExecution(id)
                }))
              ),
              tr(row)(
                theExpanders(id.id).currentColumn().map { col ⇒
                  tags.td(colspan := 12)(hiddenMap(col))
                }.getOrElse(tags.div())
              )
            )
          }
        })
      }
    ).render
  }

  def cancelExecution(id: ExecutionId) =
    OMPost[Api].cancelExecution(id).call().foreach { r ⇒
      updateExecutionInfo
    }

  def removeExecution(id: ExecutionId) =
    OMPost[Api].removeExecution(id).call().foreach { r ⇒
      updateExecutionInfo
    }

  def updateEnvErrors(environmentId: EnvironmentId, reset: Boolean) = {
    OMPost[Api].runningErrorEnvironmentData(environmentId, envErrorHistory.value.toInt, reset).call().foreach { err ⇒
      envError() = envError.now + (environmentId → err)
    }
  }

  def displaySize(size: Long, readable: String) =
    if (size == 0L) ""
    else s"($readable)"

  def visibleClass(expandID: ExpandID, columnID: ColumnID, modifier: Modifier, extraStyle: ModifierSeq = emptyMod) =
    expanderIfVisible(expandID, columnID, ex ⇒
      tags.span(omsheet.executionVisible +++ extraStyle, modifier), tags.span(extraStyle, modifier))

  val settingsDiv = bs.vForm(width := 200)(
    outputHistory.withLabel("# outputs"),
    envErrorHistory.withLabel("# environment errors")
  )

  val dialog = bs.ModalDialog(omsheet.panelWidth(92))

  dialog.header(
    div(height := 55)(
      b("Executions"),
      div(omsheet.panelHeaderSettings)(
        settingsDiv.dropdown(
        "",
        btn_default +++ glyph_settings +++ omsheet.settingsButton
      ).render
      )
    )
  )

  dialog.body(
    tags.div(ms("executionTable"))(
      executionTable
    )
  )

  dialog.footer(
    ModalDialog.closeButton(dialog, btn_default, "Close")
  )

}