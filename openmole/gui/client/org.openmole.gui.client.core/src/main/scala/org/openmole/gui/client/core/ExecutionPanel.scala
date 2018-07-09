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

import scala.util.{ Failure, Success }
import scalatags.JsDom.all._
import org.openmole.gui.client.tool.Expander._
import scalatags.JsDom._
import org.openmole.gui.ext.tool.client._

import scala.scalajs.js.timers._
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import org.openmole.gui.ext.data.{ Error ⇒ ExecError }
import org.openmole.gui.ext.data._
import org.openmole.gui.client.core.alert.BannerAlert
import org.openmole.gui.client.core.alert.BannerAlert.BannerMessage
import org.openmole.gui.client.tool.{ DynamicScrolledTextArea, Expander }
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.tool.client.Utils
import org.scalajs.dom.html.TextArea
import org.scalajs.dom.raw.{ HTMLDivElement, HTMLElement }
import rx._
import rx.async._
import rx.async.Platform._

import scala.concurrent.duration._
import scaladget.bootstrapnative.Table.{ BSTableStyle, FixedCell, ReactiveRow, SubRow, VarCell }

import concurrent.duration._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import scaladget.bootstrapnative.bsn.ScrollableText
import scaladget.bootstrapnative.bsn.ScrollableTextArea.BottomScroll

object ExecutionPanel {

  sealed trait JobView

  object CapsuleView extends JobView

  object EnvironmentView extends JobView

  sealed trait Sub

  object SubScript extends Sub

  object SubOutput extends Sub

  object SubCompile extends Sub

  object SubEnvirorment extends Sub

  implicit def idToExecutionID(id: scaladget.tools.ID): ExecutionId = ExecutionId(id)

}

import ExecutionPanel._

class ExecutionPanel {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  //  case class PanelInfo(
  //    executionInfos: Seq[(ExecutionId, ExecutionInfo)],
  //    outputsInfos:   Seq[OutputStreamData]
  //  )

  case class ExInfo(id: ExecutionId, info: Var[ExecutionInfo])

  //val execInfo = Var(PanelInfo(Seq(), Seq()))
  val staticInfo: Var[Map[ExecutionId, StaticExecutionInfo]] = Var(Map())
  val executionInfo: Var[Map[ExecutionId, ExecutionInfo]] = Var(Map())
  val outputInfo: Var[Seq[OutputStreamData]] = Var(Seq())
  val envError: Var[Map[EnvironmentId, EnvironmentErrorData]] = Var(Map())
  val tableIndexes: Var[Map[ExecutionId, Int]] = Var(Map())
  //val expanders: Var[Map[ExpandID, Expander]] = Var(Map())

  val executionsDisplayedInBanner: Var[Set[ExecutionId]] = Var(Set())

  val timerOn = Var(false)
  //val jobTables: Var[Map[ExecutionId, JobTable]] = Var(Map())

  val updating = new AtomicBoolean(false)

  case class SubRowPanels(script: Rx[TypedTag[HTMLElement]], output: Rx[TypedTag[HTMLElement]])

  val emptySubRowPanel = SubRowPanels(Rx(tags.div("")), Rx(tags.div("")))

  val subRows: Var[Map[ExecutionId, SubRowPanels]] = Var(Map())
  val expanded: Var[Map[ExecutionId, Option[Sub]]] = Var(Map())
  val subDiv: Var[Map[ExecutionId, Rx[TypedTag[HTMLElement]]]] = Var(Map())

  def subRowPanel(executionId: ExecutionId, srp: SubRowPanels, sub: Sub) = {

    subDiv.update(subDiv.now.updated(
      executionId,
      sub match {
        case SubScript ⇒ srp.script
        case SubOutput ⇒ srp.output
        case x: Any    ⇒ Rx(div(""))
      }

    ))
  }
  def subLink(s: Sub, id: ExecutionId, name: String = "", glyphicon: Glyphicon = emptyMod) = tags.span(glyphicon, cursor := "pointer", onclick := { () ⇒

    val curSub = expanded.now.get(id).flatten
    subRows.map { sr ⇒
      sr.get(id).foreach { srp ⇒
        subRowPanel(id, srp, s)
      }
    }
    expanded() = expanded.now.updated(id, curSub match {
      case Some(ss: Sub) ⇒
        if (ss == s) None
        else Some(s)
      case _ ⇒ Some(s)
    })

  })(name)

  def execTextArea(content: String): TypedTag[HTMLElement] = textarea(content, height := "300px", width := "100%")

  def execTextArea(content: Rx[String]): TypedTag[HTMLElement] = {
    val st = scrollableText(content.now, BottomScroll)
    content.trigger {
      st.setContent(content.now)
      println("DO scroll")
      st.doScroll
    }
    div(st.sRender)
  }

  lazy val executionTable = scaladget.bootstrapnative.Table(
    for {
      execMap ← executionInfo
    } yield {
      execMap.map {
        case (execID, info) ⇒
          val duration: Duration = (info.duration milliseconds)
          val h = (duration).toHours
          val m = ((duration) - (h hours)).toMinutes
          val s = (duration - (h hours) - (m minutes)).toSeconds

          val durationString =
            s"""${
              h.formatted("%d")
            }:${
              m.formatted("%02d")
            }:${
              s.formatted("%02d")
            }"""

          val completed = info.completed

          val (details, statusTag) = info match {
            case f: ExecutionInfo.Failed ⇒
              addToBanner(execID, BannerAlert.div(failedDiv(execID)).critical)
              (ExecutionDetails("0", 0, Some(f.error), f.environmentStates), tags.span(info.state))
            case f: ExecutionInfo.Finished ⇒
              addToBanner(execID, BannerAlert.div(succesDiv(execID)))
              (ExecutionDetails(ratio(f.completed, f.running, f.ready), f.running, envStates = f.environmentStates), (if (!f.clean) tags.span("cleaning") else tags.span(info.state)))
            case r: ExecutionInfo.Running ⇒ (ExecutionDetails(ratio(r.completed, r.running, r.ready), r.running, envStates = r.environmentStates), tags.span(info.state))
            case c: ExecutionInfo.Canceled ⇒
              hasBeenDisplayed(execID)
              (ExecutionDetails("0", 0, envStates = c.environmentStates), (if (!c.clean) tags.span("cleaning") else tags.span(info.state)))
            case r: ExecutionInfo.Compiling ⇒
              println("COmipiling")
              (ExecutionDetails("0", 0, envStates = r.environmentStates), tags.span(info.state))
            case r: ExecutionInfo.Preparing ⇒ (ExecutionDetails("0", 0, envStates = r.environmentStates), tags.span(info.state))
          }
          val srp = SubRowPanels(
            staticInfo.map { si ⇒
              execTextArea(si(execID).script)
            },
            //            staticInfo.map { si =>
            //              val eta = execTextArea(si(execID).script)
            //
            //              span(eta)
            //            },
            outputInfo.map { oi ⇒
              execTextArea(oi.find(_.id == execID).map {
                _.output
              }.getOrElse("")
              )
            }
          )
          subRows() = subRows.now.updated(execID, srp)

          expanded.now.get(execID).flatten.foreach { sub ⇒
            subRowPanel(execID, srp, sub)
          }

          ReactiveRow(
            execID.id,
            Seq(
              FixedCell(tags.span(subLink(SubScript, execID, staticInfo.now(execID).path.name).tooltip("Original script")), 0),
              VarCell(tags.span(tags.span(Utils.longToDate(staticInfo.now(execID).startDate)).tooltip("Starting time")), 1),
              VarCell(tags.span(glyphAndText(glyph_flash, details.running.toString).tooltip("Running jobs")), 2),
              VarCell(tags.span(glyphAndText(glyph_flag, details.ratio.toString).tooltip("Finished/Total jobs")), 3),
              VarCell(tags.span(tags.span(durationString).tooltip("Elapsed time")), 4),
              VarCell(tags.span(statusTag(executionState(info)).tooltip("Execution state")), 5),
              FixedCell(tags.span(tags.span(glyph_stats, "Env").tooltip("Computation environment details")), 6),
              VarCell(tags.span(subLink(SubOutput, execID, glyphicon = glyph_list).tooltip("Standard output")), 7),
              FixedCell(tags.span(tags.span(glyph_remove +++ ms("removeExecution"), onclick := {
                () ⇒
                  cancelExecution(execID)
              }).tooltip("Cancel execution")), 8),
              FixedCell(tags.span(tags.span(glyph_trash +++ ms("removeExecution"), onclick := {
                () ⇒
                  removeExecution(execID)
              }).tooltip("Trash execution")), 9)
            ))
      }.toSeq
    },
    subRow = Some((i: scaladget.tools.ID) ⇒
      SubRow(
        subDiv.flatMap { _.getOrElse(ExecutionId(i), Rx { div("TOTO") }) },
        expanded.map {
          _.get(ExecutionId(i)).flatten.isDefined
        }
      )
    ),
    bsTableStyle = BSTableStyle(tableStyle = inverse_table)
  )

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
        Tooltip.cleanAll
        updateExecutionInfo
      }
    }

    if (updating.compareAndSet(false, true)) {
      post()[Api].allStates(outputHistory.value.toInt).call().andThen {
        case Success((executionInfos, runningOutputData)) ⇒
          executionInfos.foreach {
            ei ⇒
              executionInfo() = executionInfo.now.updated(ei._1, ei._2)
          }
          outputInfo() = runningOutputData
          if (timerOn.now) delay
        case Failure(_) ⇒ delay
      }

      //A UTILISER DIRECTEMENT DEPUIS JOBTABLE DANS UN RX
      //      execInfo.now.executionInfos.map {
      //        _._2.environmentStates.foreach { es ⇒
      //          updateEnvErrors(es.envId)
      //        }
      //      }
    }
  }

  //def atLeastOneNotDisplayed = execInfo.now.executionInfos.exists { ex ⇒ !executionsDisplayedInBanner.now.contains(ex._1) }

  def updateStaticInfos = post()[Api].staticInfos.call().foreach {
    s ⇒
      staticInfo() = s.toMap
      setTimeout(0) {
        updateExecutionInfo
      }
  }

  //  def doScrolls = {
  //    val scrollables =
  //      Seq(outputTextAreas.now, scriptTextAreas.now, errorTextAreas.now).flatMap {
  //        _.values
  //      } ++
  //        envErrorPanels.now.flatMap { e ⇒ Seq(e._2.scrollableTable, e._2.scrollableStack) }.toSeq
  //
  //    scrollables.foreach {
  //      _.doScroll
  //    }
  //  }

  case class ExecutionDetails(
    ratio:     String,
    running:   Long,
    error:     Option[ExecError]     = None,
    envStates: Seq[EnvironmentState] = Seq(),
    outputs:   String                = ""
  )

  //  val scriptTextAreas: Var[Map[ExecutionId, ScrollableText]] = Var(Map())
  //  val errorTextAreas: Var[Map[ExecutionId, ScrollableText]] = Var(Map())
  //  val outputTextAreas: Var[Map[ExecutionId, ScrollableText]] = Var(Map())
  //  val envErrorPanels: Var[Map[EnvironmentId, EnvironmentErrorPanel]] = Var(Map())

  //  val envErrorVisible: Var[Seq[EnvironmentId]] = Var(Seq())

  //  def toggleEnvironmentErrorPanel(envID: EnvironmentId) = { () ⇒
  //    if (envErrorVisible.now.contains(envID)) envErrorVisible() = envErrorVisible.now.filterNot {
  //      _ == envID
  //    }
  //    else envErrorVisible() = envErrorVisible.now :+ envID
  //
  //  }

  //  def staticPanel[T, I <: org.openmole.gui.ext.data.ID](id: I, panelMap: Var[Map[I, T]], builder: () ⇒ T, appender: T ⇒ Unit = (t: T) ⇒ {}): T = {
  //    Tooltip.cleanAll
  //    if (panelMap.now.isDefinedAt(id)) {
  //      val t = panelMap.now(id)
  //      appender(t)
  //      t
  //    }
  //    else {
  //      val toBeAdded = builder()
  //      panelMap() = panelMap.now + (id → toBeAdded)
  //      toBeAdded
  //    }
  //  }

  //  def jobTable(id: ExecutionId) = {
  //    println("JTABLES " + jobTables.now)
  //    if (jobTables.now.isDefinedAt(id)) {
  //      println("IFFFFFF")
  //      jobTables.now(id)
  //    }
  //    else {
  //      println("ELLLLSE")
  //      val jTable = JobTable(id)
  //      println("instancied")
  //      jobTables() = jobTables.now.updated(id, jTable)
  //      println("JTABDES  " + jobTables.now)
  //      jTable
  //    }
  //  }

  val envLevel: Var[ErrorStateLevel] = Var(ErrorLevel())

  val outputHistory = inputTag("500")(placeholder := "# outputs").render
  val envErrorHistory = inputTag("500")(placeholder := "# environment errors").render

  def ratio(completed: Long, running: Long, ready: Long) = s"${
    completed
  } / ${
    completed + running + ready
  }"

  def glyphAndText(mod: ModifierSeq, text: String) = tags.span(
    tags.span(mod),
    s" $text"
  )

  def hasBeenDisplayed(id: ExecutionId) = executionsDisplayedInBanner() = (executionsDisplayedInBanner.now + id)

  def addToBanner(id: ExecutionId, bannerMessage: BannerMessage) = {
    if (!executionsDisplayedInBanner.now.contains(id)) {
      BannerAlert.register(bannerMessage)
      hasBeenDisplayed(id)
    }
  }

  //<<<<<<< Updated upstream
  //  lazy val executionTable = {
  //    val scriptID: ColumnID = "script"
  //    val envID: ColumnID = "env"
  //    val errorID: ColumnID = "error"
  //    val outputStreamID: ColumnID = "outputStream"
  //    tags.table(tableClass)(
  //      thead,
  //      Rx {
  //        tbody({
  //          for {
  //            (id, executionInfo) ← execInfo().executionInfos.sortBy { case (execId, _) ⇒ staticInfo.now(execId).startDate }.reverse
  //          } yield {
  //            if (!expanders().keys.exists(ex ⇒ ex == id.id)) {
  //              expanders() = expanders() ++ Map(id.id → new Expander(id.id))
  //              jobViews() = jobViews() ++ Map(id → CapsuleView)
  //            }
  //            val theExpanders = expanders()
  //            val duration: Duration = (executionInfo.duration milliseconds)
  //            val h = (duration).toHours
  //            val m = ((duration) - (h hours)).toMinutes
  //            val s = (duration - (h hours) - (m minutes)).toSeconds
  //
  //            val durationString = s"""${h.formatted("%d")}:${m.formatted("%02d")}:${s.formatted("%02d")}"""
  //
  //            val completed = executionInfo.completed
  //
  //            def failedDiv(ex: Expander) = div(
  //              s"Your simulation ${staticInfo.now(id).path.name} ", a("failed", bold(WHITE) +++ pointer, onclick := { () ⇒
  //                BannerAlert.clear
  //                dialog.show
  //                ex.update(errorID)
  //              })
  //            )
  //
  //            val succesDiv = div(
  //              s"Your simulation ${staticInfo.now(id).path.name} has ", a("finished", bold(WHITE) +++ pointer, onclick := { () ⇒
  //                BannerAlert.clear
  //                dialog.show
  //              })
  //            )
  //
  //            val details = executionInfo match {
  //              case f: ExecutionInfo.Failed ⇒
  //                ExecutionDetails("0", 0, Some(f.error), f.environmentStates)
  //              case f: ExecutionInfo.Finished ⇒
  //                addToBanner(id, BannerAlert.div(succesDiv))
  //                ExecutionDetails(ratio(f.completed, f.running, f.ready), f.running, envStates = f.environmentStates)
  //              case r: ExecutionInfo.Running ⇒ ExecutionDetails(ratio(r.completed, r.running, r.ready), r.running, envStates = r.environmentStates)
  //              case c: ExecutionInfo.Canceled ⇒
  //                hasBeenDisplayed(id)
  //                ExecutionDetails("0", 0, envStates = c.environmentStates)
  //              case r: ExecutionInfo.Compiling ⇒ ExecutionDetails("0", 0, envStates = r.environmentStates)
  //              case r: ExecutionInfo.Preparing ⇒ ExecutionDetails("0", 0, envStates = r.environmentStates)
  //            }
  //
  //            val jobTable = JobTable(id, executionInfo)
  //            val scriptLink = expander(id.id, ex ⇒ ex.getLink(staticInfo.now(id).path.name, scriptID))
  //            val envLink = expander(id.id, ex ⇒ ex.getGlyph(glyph_stats, "Env", envID))
  //            val stateLink = expander(id.id, ex ⇒
  //              executionInfo match {
  //                case f: ExecutionInfo.Failed ⇒
  //                  addToBanner(id, BannerAlert.div(failedDiv(ex)).critical)
  //                  ex.getLink(executionInfo.state, errorID).render
  //                case _ ⇒
  //                  executionInfo match {
  //                    case executionInfo: ExecutionInfo.Finished if !executionInfo.clean ⇒ tags.span("cleaning").render
  //                    case executionInfo: ExecutionInfo.Canceled if !executionInfo.clean ⇒ tags.span("cleaning").render
  //                    case executionInfo: ExecutionInfo.Failed if !executionInfo.clean ⇒ tags.span("cleaning").render
  //                    case _ ⇒ tags.span(executionInfo.state).render
  //                  }
  //              })
  //            val outputLink = expander(id.id, ex ⇒ ex.getGlyph(glyph_list, "", outputStreamID, () ⇒ doScrolls))
  //
  //            val hiddenMap: Map[ColumnID, Modifier] = Map(
  //              scriptID → staticPanel(id, scriptTextAreas,
  //                () ⇒ scrollableText(staticInfo.now(id).script)).view,
  //
  //              envID → tr(row)(jobTable.render) //{ details.envStates.map { e ⇒
  //              //  tags.table(width := "100%")(
  //              //   thead,
  //              //  tbody(
  //              // Seq(
  //              //                        tr(row +++ (fontSize := 14))(
  //              //                          td(colMD(1) +++ textCenter)(tags.span(e.taskName)),
  //              //                          td(colMD(2) +++ textCenter)(tags.span(CoreUtils.approximatedYearMonthDay(e.executionActivity.executionTime))).tooltip("Full computation time"),
  //              //                          td(colMD(2) +++ textCenter)(glyphAndText(glyph_upload, s" ${e.networkActivity.uploadingFiles} ${displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize)}")).tooltip("Uploads"),
  //              //                          td(colMD(2) +++ textCenter)(glyphAndText(glyph_download, s" ${e.networkActivity.downloadingFiles} ${displaySize(e.networkActivity.downloadedSize, e.networkActivity.readableDownloadedSize)}")).tooltip("Downloads"),
  //              //                          td(colMD(1) +++ textCenter)(glyphAndText(glyph_road +++ (paddingBottom := 7), e.submitted.toString)).tooltip("Submitted jobs"),
  //              //                          td(colMD(1) +++ textCenter)(glyphAndText(glyph_flash +++ (paddingBottom := 7), e.running.toString)).tooltip(("Running jobs")),
  //              //                          td(colMD(1) +++ textCenter)(glyphAndText(glyph_flag +++ (paddingBottom := 7), e.done.toString)).tooltip("Finished jobs"),
  //              //                          td(colMD(1) +++ textCenter)(glyphAndText(glyph_fire +++ (paddingBottom := 7), e.failed.toString)).tooltip("Failed jobs"),
  //              //                          td(colMD(3) +++ textCenter)(({
  //              //                            if (envErrorVisible().contains(e.envId)) {
  //              //                              tags.div(rowLayout +++ (width := 100))(
  //              //                                buttonGroup(columnLayout +++ (width := 80))(
  //              //                                  buttonIcon(glyphicon = glyph_refresh, todo = { () ⇒ updateEnvErrors(e.envId) }).tooltip("Refresh environment errors"),
  //              //                                  buttonIcon(buttonStyle = btn_default, glyphicon = glyph_repeat, todo = { () ⇒ { clearEnvErrors(e.envId) } }).tooltip("Reset environment errors")
  //              //                                ),
  //              //                                tags.span(onclick := toggleEnvironmentErrorPanel(e.envId), columnLayout +++ closeDetails)(raw("&#215"))
  //              //                              )
  //              //                            }
  //              //                            else tags.span(omsheet.color(BLUE) +++ ((envErrorVisible().contains(e.envId)), ms(" executionVisible"), emptyMod))(
  //              //                              pointer, onclick := toggleEnvironmentErrorPanel(e.envId)
  //              //                            )("details")
  //              //                          })).tooltip("Error details")
  //              //                        ),
  //              //                        tr(row)(
  //              //                          {
  //              //                            td(colMD(12) +++ textCenter +++ (!envErrorVisible().contains(e.envId), omsheet.displayOff, emptyMod))(
  //              //                              colspan := 12,
  //              //                              staticPanel(e.envId, envErrorPanels,
  //              //                                () ⇒ new EnvironmentErrorPanel,
  //              //                                (ep: EnvironmentErrorPanel) ⇒
  //              //                                  ep.setErrors(envError().getOrElse(e.envId, EnvironmentErrorData.empty))).view
  //              //                            )
  //              //                          }
  //              //                        )
  //              //)
  //              //  )
  //              //  )
  //              //  }
  //              //    }
  //              ,
  //
  //              errorID →
  //                div(
  //                  omsheet.monospace,
  //                  staticPanel(
  //                    id,
  //                    errorTextAreas,
  //                    () ⇒ scrollableText(),
  //                    (sT: ScrollableText) ⇒ sT.setContent(new String(details.error.map {
  //                      _.stackTrace
  //                    }.getOrElse("")))
  //                  ).view
  //                ),
  //              outputStreamID → staticPanel(
  //                id,
  //                outputTextAreas,
  //                () ⇒ scrollableText("", BottomScroll),
  //                (sT: ScrollableText) ⇒ sT.setContent(
  //                  execInfo().outputsInfos.filter {
  //                    _.id == id
  //                  }.map {
  //                    _.output
  //                  }.mkString("\n")
  //                )
  //              ).view
  //            )
  //
  //            Seq(
  //              tr(row +++ omsheet.executionTable, colspan := 12)(
  //                td(colMD(2), pointer +++ textCenter)(visibleClass(id.id, scriptID, scriptLink)).tooltip("Original script"),
  //                td(colMD(2) +++ textCenter)(div(Utils.longToDate(staticInfo.now(id).startDate))).tooltip("Starting time"),
  //                td(colMD(2) +++ textCenter)(glyphAndText(glyph_flash, details.running.toString)).tooltip("Running jobs"),
  //                td(colMD(2) +++ textCenter)(glyphAndText(glyph_flag, details.ratio.toString)).tooltip("Finished/Total jobs"),
  //                td(colMD(1) +++ textCenter)(div(durationString)).tooltip("Elapsed time"),
  //                td(colMD(1) +++ textCenter)(visibleClass(id.id, errorID, stateLink, omsheet.executionState(executionInfo))).tooltip("Execution state"),
  //                td(colMD(1) +++ textCenter, pointer)(visibleClass(id.id, envID, envLink)).tooltip("Computation environment details"),
  //                td(colMD(1) +++ textCenter, pointer)(visibleClass(id.id, outputStreamID, outputLink)).tooltip("Standard output"),
  //                td(colMD(1) +++ textCenter)(tags.span(glyph_remove +++ ms("removeExecution"), onclick := { () ⇒
  //                  cancelExecution(id)
  //                })).tooltip("Cancel execution"),
  //                td(colMD(1) +++ textCenter)(tags.span(glyph_trash +++ ms("removeExecution"), onclick := { () ⇒
  //                  removeExecution(id)
  //                })).tooltip("Trash execution")
  //              ),
  //              tr(row)(
  //                theExpanders(id.id).currentColumn().map { col ⇒
  //                  tags.td(colspan := 12)(hiddenMap(col))
  //                }.getOrElse(tags.div())
  //              )
  //            )
  //=======
  def failedDiv(id: ExecutionId) = div(
    s"Your simulation ${
      staticInfo.now(id).path.name
    } ", a("failed", bold(WHITE) +++ pointer, onclick := {
      () ⇒
        BannerAlert.clear
        dialog.show
    })
  )

  def succesDiv(id: ExecutionId) = div(
    s"Your simulation ${
      staticInfo.now(id).path.name
    } has ", a("finished", bold(WHITE) +++ pointer, onclick := {
      () ⇒
        BannerAlert.clear
        dialog.show
    })
  )

  //    tags.table(tableClass)(
  //      thead,
  //      Rx {
  //        tbody({
  //          for {
  //            (id, executionInfo) ← execInfo().executionInfos.sortBy { case (execId, _) ⇒ staticInfo.now(execId).startDate }.reverse
  //          } yield {
  //            if (!expanders().keys.exists(ex ⇒ ex == id.id)) {
  //              expanders() = expanders() ++ Map(id.id → new Expander(id.id))
  //            }
  //            val theExpanders = expanders()
  //            val duration: Duration = (executionInfo.duration milliseconds)
  //            val h = (duration).toHours
  //            val m = ((duration) - (h hours)).toMinutes
  //            val s = (duration - (h hours) - (m minutes)).toSeconds
  //
  //            val durationString = s"""${h.formatted("%d")}:${m.formatted("%02d")}:${s.formatted("%02d")}"""
  //
  //            val completed = executionInfo.completed
  //
  //            def failedDiv(ex: Expander) = div(
  //              s"Your simulation ${staticInfo.now(id).path.name} ", a("failed", bold(WHITE) +++ pointer, onclick := { () ⇒
  //                BannerAlert.clear
  //                dialog.show
  //                ex.update(errorID)
  //              })
  //            )
  //
  //            val succesDiv = div(
  //              s"Your simulation ${staticInfo.now(id).path.name} has ", a("finished", bold(WHITE) +++ pointer, onclick := { () ⇒
  //                BannerAlert.clear
  //                dialog.show
  //              })
  //            )
  //
  //            val details = executionInfo match {
  //              case f: ExecutionInfo.Failed ⇒
  //                ExecutionDetails("0", 0, Some(f.error), f.environmentStates)
  //              case f: ExecutionInfo.Finished ⇒
  //                addToBanner(id, BannerAlert.div(succesDiv))
  //                ExecutionDetails(ratio(f.completed, f.running, f.ready), f.running, envStates = f.environmentStates)
  //              case r: ExecutionInfo.Running ⇒ ExecutionDetails(ratio(r.completed, r.running, r.ready), r.running, envStates = r.environmentStates)
  //              case c: ExecutionInfo.Canceled ⇒
  //                hasBeenDisplayed(id)
  //                ExecutionDetails("0", 0, envStates = c.environmentStates)
  //              case r: ExecutionInfo.Launching ⇒ ExecutionDetails("0", 0, envStates = r.environmentStates)
  //            }
  //
  //            val scriptLink = expander(id.id, ex ⇒ ex.getLink(staticInfo.now(id).path.name, scriptID))
  //            val envLink = expander(id.id, ex ⇒ ex.getGlyph(glyph_stats, "Env", envID))
  //            val stateLink = expander(id.id, ex ⇒
  //              executionInfo match {
  //                case f: ExecutionInfo.Failed ⇒
  //                  addToBanner(id, BannerAlert.div(failedDiv(ex)).critical)
  //                  ex.getLink(executionInfo.state, errorID).render
  //                case _ ⇒
  //                  executionInfo match {
  //                    case executionInfo: ExecutionInfo.Finished if !executionInfo.clean ⇒ tags.span("cleaning").render
  //                    case executionInfo: ExecutionInfo.Canceled if !executionInfo.clean ⇒ tags.span("cleaning").render
  //                    case executionInfo: ExecutionInfo.Failed if !executionInfo.clean ⇒ tags.span("cleaning").render
  //                    case _ ⇒ tags.span(executionInfo.state).render
  //                  }
  //              })
  //            val outputLink = expander(id.id, ex ⇒ ex.getGlyph(glyph_list, "", outputStreamID, () ⇒ doScrolls))
  //
  //            val hiddenMap: Map[ColumnID, Modifier] = Map(
  //              scriptID → staticPanel(id, scriptTextAreas,
  //                () ⇒ scrollableText(staticInfo.now(id).script)).view,
  //              envID → tr(row)(
  //                executionInfo match {
  //                  case executionInfo: ExecutionInfo.Running ⇒ jobTable(id).render
  //                  case _                                    ⇒ Waiter.waiter
  //                }
  //
  //              ),
  //              errorID →
  //                div(
  //                  omsheet.monospace,
  //                  staticPanel(
  //                    id,
  //                    errorTextAreas,
  //                    () ⇒ scrollableText(),
  //                    (sT: ScrollableText) ⇒ sT.setContent(new String(details.error.map {
  //                      _.stackTrace
  //                    }.getOrElse("")))
  //                  ).view
  //                ),
  //              outputStreamID → staticPanel(
  //                id,
  //                outputTextAreas,
  //                () ⇒ scrollableText("", BottomScroll),
  //                (sT: ScrollableText) ⇒ sT.setContent(
  //                  execInfo().outputsInfos.filter {
  //                    _.id == id
  //                  }.map {
  //                    _.output
  //                  }.mkString("\n")
  //                )
  //              ).view
  //            )
  //
  //            Seq(
  //              tr(row +++ omsheet.executionTable, colspan := 12)(
  //                td(colMD(2), pointer +++ textCenter)(visibleClass(id.id, scriptID, scriptLink)).tooltip("Original script"),
  //                td(colMD(2) +++ textCenter)(div(Utils.longToDate(staticInfo.now(id).startDate))).tooltip("Starting time"),
  //                td(colMD(2) +++ textCenter)(glyphAndText(glyph_flash, details.running.toString)).tooltip("Running jobs"),
  //                td(colMD(2) +++ textCenter)(glyphAndText(glyph_flag, details.ratio.toString)).tooltip("Finished/Total jobs"),
  //                td(colMD(1) +++ textCenter)(div(durationString)).tooltip("Elapsed time"),
  //                td(colMD(1) +++ textCenter)(visibleClass(id.id, errorID, stateLink, omsheet.executionState(executionInfo))).tooltip("Execution state"),
  //                td(colMD(1) +++ textCenter, pointer)(visibleClass(id.id, envID, envLink)).tooltip("Computation environment details"),
  //                td(colMD(1) +++ textCenter, pointer)(visibleClass(id.id, outputStreamID, outputLink)).tooltip("Standard output"),
  //                td(colMD(1) +++ textCenter)(tags.span(glyph_remove +++ ms("removeExecution"), onclick := { () ⇒
  //                  cancelExecution(id)
  //                })).tooltip("Cancel execution"),
  //                td(colMD(1) +++ textCenter)(tags.span(glyph_trash +++ ms("removeExecution"), onclick := { () ⇒
  //                  removeExecution(id)
  //                })).tooltip("Trash execution")
  //              ),
  //              tr(row)(
  //                theExpanders(id.id).currentColumn().map { col ⇒
  //                  tags.td(colspan := 12)(hiddenMap(col))
  //                }.getOrElse(tags.div())
  //              )
  //            )
  //          }
  //        })
  //      }
  //    ).render

  def cancelExecution(id: ExecutionId) = {
    // setIDTabInStandBy(id)
    post()[Api].cancelExecution(id).call().foreach {
      r ⇒
        updateExecutionInfo
    }
  }

  def removeExecution(id: ExecutionId) = {
    //setIDTabInStandBy(id)
    post()[Api].removeExecution(id).call().foreach {
      r ⇒
        updateExecutionInfo
    }
    envError() = envError.now.filterNot {
      e ⇒ e._1.executionId == id
    }
    //    envErrorPanels() = envErrorPanels.now.filterNot {
    //      e ⇒ e._1.executionId == id
    //    }
    executionsDisplayedInBanner() = executionsDisplayedInBanner.now - id
  }

  def clearEnvErrors(environmentId: EnvironmentId) =
    post()[Api].clearEnvironmentErrors(environmentId).call().foreach {
      _ ⇒
        envError() = envError.now - environmentId
    }

  def updateEnvErrors(environmentId: EnvironmentId) =
    post()[Api].runningErrorEnvironmentData(environmentId, envErrorHistory.value.toInt).call().foreach {
      err ⇒
        println("ERR")
        envError() = envError.now + (environmentId → err)
    }

  //  def visibleClass(expandID: ExpandID, columnID: ColumnID, modifier: Modifier, extraStyle: ModifierSeq = emptyMod) =
  //    expanderIfVisible(expandID, columnID, ex ⇒
  //      tags.span(omsheet.executionVisible +++ extraStyle, modifier), tags.span(extraStyle, modifier))

  val settingsForm = vForm(width := 200)(
    outputHistory.withLabel("# outputs"),
    envErrorHistory.withLabel("# environment errors")
  )

  val dialog = ModalDialog(
    omsheet.panelWidth(92),
    onopen = () ⇒ {
      setTimerOn
      //      closeAllExpanders
      updateStaticInfos
    },
    onclose = () ⇒ {
      setTimerOff
      Tooltip.cleanAll
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
      executionTable.render
    )
  )

  dialog.footer(
    ModalDialog.closeButton(dialog, btn_default, "Close")
  )

}