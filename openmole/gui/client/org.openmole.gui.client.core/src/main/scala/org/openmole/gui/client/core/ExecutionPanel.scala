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

import scala.util.{ Failure, Success }
import scalatags.JsDom.all._
import org.openmole.gui.client.tool.Expander._

import scalatags.JsDom._
import org.openmole.gui.ext.tool.client.JsRxTags._
import org.openmole.gui.ext.tool.client._

import scala.scalajs.js.timers._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import sheet._
import autowire._
import org.openmole.gui.ext.data.{ Error ⇒ ExecError }
import org.openmole.gui.ext.data._
import bs._
import org.openmole.gui.client.core.alert.BannerAlert
import org.openmole.gui.client.core.alert.BannerAlert.BannerMessage
import org.openmole.gui.client.core.files.TreeNodeTabs.StandBy
import org.openmole.gui.client.tool.Expander
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.tool.client.Utils
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
  val executionsDisplayedInBanner: Var[Seq[ExecutionId]] = Var(Seq())
  val timerOn = Var(false)

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

  def setTimerOn = {
    updating.set(false)
    timerOn() = true
  }

  def setTimerOff = {
    timerOn() = false
  }

  def updateExecutionInfo: Unit = {

    def delay = {
      updating.set(false)
      setTimeout(5000) {
        if (atLeastOneNotDisplayed) updateExecutionInfo
      }
    }

    if (updating.compareAndSet(false, true)) {
      post()[Api].allStates(outputHistory.value.toInt).call().andThen {
        case Success((executionInfos, runningOutputData)) ⇒
          execInfo() = PanelInfo(executionInfos, runningOutputData)
          doScrolls
          if (timerOn.now) delay
        case Failure(_) ⇒ delay
      }
    }
  }

  def atLeastOneNotDisplayed = execInfo.now.executionInfos.exists { ex ⇒ !executionsDisplayedInBanner.now.contains(ex._1) }

  def updateStaticInfos = post()[Api].staticInfos.call().foreach { s ⇒
    staticInfo() = s.toMap
    setTimeout(0) {
      updateExecutionInfo
    }
  }

  def doScrolls = {
    val scrollables =
      Seq(outputTextAreas.now, scriptTextAreas.now, errorTextAreas.now).flatMap {
        _.values
      } ++
        envErrorPanels.now.flatMap { e ⇒ Seq(e._2.scrollableTable, e._2.scrollableStack) }.toSeq

    scrollables.foreach {
      _.doScroll
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

  def staticPanel[T, I <: org.openmole.gui.ext.data.ID](id: I, panelMap: Var[Map[I, T]], builder: () ⇒ T, appender: T ⇒ Unit = (t: T) ⇒ {}): T = {
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

  def hasBeenDisplayed(id: ExecutionId) = executionsDisplayedInBanner() = (executionsDisplayedInBanner.now :+ id).distinct

  def addToBanner(id: ExecutionId, bannerMessage: BannerMessage) = {
    if (!executionsDisplayedInBanner.now.contains(id)) {
      BannerAlert.register(bannerMessage)
      hasBeenDisplayed(id)
    }
  }

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

            def failedDiv(ex: Expander) = div(
              s"Your simulation ${staticInfo.now(id).path.name} ", a("failed", bold(WHITE) +++ pointer, onclick := { () ⇒
                BannerAlert.clear
                dialog.show
                ex.update(errorID)
              })
            )

            val succesDiv = div(
              s"Your simulation ${staticInfo.now(id).path.name} has ", a("finished", bold(WHITE) +++ pointer, onclick := { () ⇒
                BannerAlert.clear
                dialog.show
              })
            )

            val details = executionInfo match {
              case f: Failed ⇒
                ExecutionDetails("0", 0, Some(f.error), f.environmentStates)
              case f: Finished ⇒
                addToBanner(id, BannerAlert.div(succesDiv))
                ExecutionDetails(ratio(f.completed, f.running, f.ready), f.running, envStates = f.environmentStates)
              case r: Running ⇒ ExecutionDetails(ratio(r.completed, r.running, r.ready), r.running, envStates = r.environmentStates)
              case c: Canceled ⇒
                hasBeenDisplayed(id)
                ExecutionDetails("0", 0, envStates = c.environmentStates)
              case r: Ready ⇒ ExecutionDetails("0", 0, envStates = r.environmentStates)
            }

            val scriptLink = expander(id.id, ex ⇒ ex.getLink(staticInfo.now(id).path.name, scriptID))
            val envLink = expander(id.id, ex ⇒ ex.getGlyph(glyph_stats, "Env", envID))
            val stateLink = expander(id.id, ex ⇒
              executionInfo match {
                case f: Failed ⇒
                  addToBanner(id, BannerAlert.div(failedDiv(ex)).critical)
                  ex.getLink(executionInfo.state, errorID).render
                case _ ⇒ tags.span(executionInfo.state).render
              })
            val outputLink = expander(id.id, ex ⇒ ex.getGlyph(glyph_list, "", outputStreamID, () ⇒ doScrolls))

            val hiddenMap: Map[ColumnID, Modifier] = Map(
              scriptID → staticPanel(id, scriptTextAreas,
                () ⇒ scrollableText(staticInfo.now(id).script)).view,

              envID → {
                details.envStates.map { e ⇒
                  tags.table(width := "100%")(
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
                          td(colMD(3))(({
                            if (envErrorVisible().contains(e.envId)) {
                              tags.div(rowLayout +++ (width := 100))(
                                bs.buttonGroup(columnLayout +++ (width := 80))(
                                  bs.button(glyphicon = glyph_refresh, todo = () ⇒ updateEnvErrors(e.envId, false)).tooltip("Refresh environment errors"),
                                  bs.button(buttonStyle = btn_default, glyphicon = glyph_repeat, todo = () ⇒ updateEnvErrors(e.envId, false)).tooltip("Reset environment errors")
                                ),
                                tags.span(onclick := toggleEnvironmentErrorPanel(e.envId), columnLayout +++ closeDetails)(raw("&#215"))
                              )
                            }
                            else tags.span(omsheet.color(BLUE) +++ ((envErrorVisible().contains(e.envId)), ms(" executionVisible"), emptyMod))(
                              sheet.pointer +++ omsheet.bold, onclick := toggleEnvironmentErrorPanel(e.envId)
                            )("details")
                          }))
                        ),
                        tr(row)(
                          {
                            td(colMD(12) +++ (!envErrorVisible().contains(e.envId), omsheet.displayOff, emptyMod))(
                              colspan := 12,
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

  def toggleEnvironmentErrorPanel(envID: EnvironmentId) = { () ⇒
    if (envErrorVisible.now.contains(envID)) envErrorVisible() = envErrorVisible.now.filterNot {
      _ == envID
    }
    else envErrorVisible() = envErrorVisible.now :+ envID
  }

  private def setIDTabInStandBy(id: ExecutionId) =
    staticInfo.now.get(id).foreach { st ⇒ panels.treeNodeTabs.set(st.path, StandBy) }

  def cancelExecution(id: ExecutionId) = {
    setIDTabInStandBy(id)
    post()[Api].cancelExecution(id).call().foreach { r ⇒
      updateExecutionInfo
    }
  }

  def removeExecution(id: ExecutionId) = {
    setIDTabInStandBy(id)
    post()[Api].removeExecution(id).call().foreach { r ⇒
      updateExecutionInfo
    }
  }

  def updateEnvErrors(environmentId: EnvironmentId, reset: Boolean) = {
    post()[Api].runningErrorEnvironmentData(environmentId, envErrorHistory.value.toInt, reset).call().foreach { err ⇒
      envError() = envError.now + (environmentId → err)
    }
  }

  def displaySize(size: Long, readable: String) =
    if (size == 0L) ""
    else s"($readable)"

  def visibleClass(expandID: ExpandID, columnID: ColumnID, modifier: Modifier, extraStyle: ModifierSeq = emptyMod) =
    expanderIfVisible(expandID, columnID, ex ⇒
      tags.span(omsheet.executionVisible +++ extraStyle, modifier), tags.span(extraStyle, modifier))

  val settingsForm = bs.vForm(width := 200)(
    outputHistory.withLabel("# outputs"),
    envErrorHistory.withLabel("# environment errors")
  )

  val dialog = bs.ModalDialog(
    omsheet.panelWidth(92),
    onopen = () ⇒ {
      setTimerOn
      closeAllExpanders
      updateStaticInfos
    },
    onclose = () ⇒ {
      setTimerOff
    }
  )

  dialog.header(
    div(height := 55)(
      b("Executions"),
      div(omsheet.panelHeaderSettings)(
        settingsForm.dropdown(
        buttonModifierSeq = btn_default,
        buttonIcon = glyph_settings
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