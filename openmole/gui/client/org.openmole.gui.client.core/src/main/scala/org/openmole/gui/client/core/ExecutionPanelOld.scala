//package org.openmole.gui.client.core
////
/////*
//// * Copyright (C) 17/05/15 // mathieu.leclaire@openmole.org
//// *
//// * This program is free software: you can redistribute it and/or modify
//// * it under the terms of the GNU Affero General Public License as published by
//// * the Free Software Foundation, either version 3 of the License, or
//// * (at your option) any later version.
//// *
//// * This program is distributed in the hope that it will be useful,
//// * but WITHOUT ANY WARRANTY; without even the implied warranty of
//// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//// * GNU Affero General Public License for more details.
//// *
//// * You should have received a copy of the GNU General Public License
//// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
//// */
//
//import java.util.concurrent.atomic.AtomicBoolean
//
//import scala.util.{ Failure, Success }
//import org.openmole.gui.client.ext._
//
//import scala.scalajs.js.timers._
//import scala.concurrent.ExecutionContext.Implicits.global
//import org.openmole.gui.shared.data.{ ErrorData ⇒ ExecError }
//import org.openmole.gui.shared.data.*
//import org.openmole.gui.client.core.alert.{ BannerAlert, BannerLevel }
//import org.openmole.gui.client.core.files.TreeNodeTabs
//import org.openmole.gui.client.tool.OMTags
//import org.openmole.gui.client.ext.Utils
//import org.openmole.gui.shared.data.ExecutionInfo.Failed
//import org.scalajs.dom.raw.{ HTMLDivElement, HTMLElement, HTMLSpanElement }
//import com.raquo.laminar.api.L._
//
//import concurrent.duration._
//import scaladget.bootstrapnative.bsn._
//import scaladget.tools._
//
//object ExecutionPanel {
//
//  sealed trait JobView
//
//  object CapsuleView extends JobView
//
//  object EnvironmentView extends JobView
//
//  sealed trait Sub
//
//  object SubScript extends Sub
//
//  object SubOutput extends Sub
//
//  object SubCompile extends Sub
//
//  object SubEnvironment extends Sub
//
//  implicit def idToExecutionID(id: scaladget.tools.Utils.ID): ExecutionId = ExecutionId(id)
//
//}
//
//import ExecutionPanel._
//
//class ExecutionPanel(
//  //setEditorErrors: (SafePath, Seq[ErrorWithLocation]) ⇒ Unit,
//  bannerAlert: BannerAlert) {
//  div("EXE PANEL")
//  //
//  //  case class ExInfo(id: ExecutionId, info: Var[ExecutionInfo])
//  //
//  //  val staticInfo: Var[Map[ExecutionId, StaticExecutionInfo]] = Var(Map())
//    val executionInfo: Var[Map[ExecutionId, ExecutionInfo]] = Var(Map())
//  //
//  //  val outputInfo: Var[Seq[OutputStreamData]] = Var(Seq())
//  //  val jobTables: Var[Map[ExecutionId, JobTable]] = Var(Map())
//  //
//  //  val executionsDisplayedInBanner: Var[Set[ExecutionId]] = Var(Set())
//  //
//  //  val timerOn = Var(false)
//  //
//  //  val updating = new AtomicBoolean(false)
//  //
//  //  case class SubRowPanels(
//  //    script:      Rx[TypedTag[HTMLElement]],
//  //    output:      Rx[TypedTag[HTMLElement]],
//  //    failedStack: Rx[TypedTag[HTMLElement]],
//  //    environment: Rx[TypedTag[HTMLElement]])
//  //
//  //  val emptySubRowPanel = SubRowPanels(Rx(div("")), Rx(div("")), Rx(div("")), Rx(div("")))
//  //
//  //  val subRows: Var[Map[ExecutionId, SubRowPanels]] = Var(Map())
//  //  val expanded: Var[Map[ExecutionId, Option[Sub]]] = Var(Map())
//  //  val subDiv: Var[Map[ExecutionId, Rx[HtmlElement]]] = Var(Map())
//  //
//  //  def subRowPanel(executionId: ExecutionId, srp: SubRowPanels, sub: Sub) = {
//  //    subDiv.update(subDiv.now.updated(
//  //      executionId,
//  //      sub match {
//  //        case SubScript      ⇒ srp.script
//  //        case SubOutput      ⇒ srp.output
//  //        case SubCompile     ⇒ srp.failedStack
//  //        case SubEnvironment ⇒ srp.environment
//  //      }
//  //
//  //    ))
//  //  }
//  //
//  //  def currentSub(id: ExecutionId) = expanded.now.get(id).flatten
//  //
//  //  def subLink(s: Sub, id: ExecutionId, name: String = "", glyphicon: Glyphicon = emptyMod, defaultModifier: HESetters = Seq(color := WHITE), selectedModifier: HESetters = Seq(color := BLUE, fontWeight.bold)) =
//  //    span(Rx {
//  //      span(glyphicon, pointer,
//  //        {
//  //          if (expanded().get(id).flatten == Some(s)) selectedModifier
//  //          else defaultModifier
//  //        },
//  //        onClick --> { _ ⇒
//  //          subRows.now.get(id).foreach { srp ⇒
//  //            subRowPanel(id, srp, s)
//  //          }
//  //
//  //          expanded.set(expanded.now.updated(id, currentSub(id) match {
//  //            case Some(ss: Sub) ⇒
//  //              if (ss == s) None
//  //              else Some(s)
//  //            case _ ⇒ Some(s)
//  //          }))
//  //
//  //        },
//  //        name)
//  //    }
//  //    )
//  //
//  //  def execTextArea(content: String): HtmlElement = textarea(content, height := "300px", width := "100%", color := "#222")
//  //
//  //  def execTextArea(content: Rx[String]): HtmlElement = {
//  //    val st = scrollableText(content.now, BottomScroll)
//  //    content.trigger {
//  //      st.setContent(content.now)
//  //      st.doScroll
//  //    }
//  //    div(st.sRender)
//  //  }
//  //
//  //  lazy val executionTable = scaladget.bootstrapnative.Table(
//  //    for {
//  //      execMap ← executionInfo
//  //      staticInf ← staticInfo
//  //    } yield {
//  //      execMap.toSeq.sortBy(e ⇒ staticInf(e._1).startDate).map {
//  //        case (execID, info) ⇒
//  //          val duration: Duration = (info.duration milliseconds)
//  //          val h = (duration).toHours
//  //          val m = ((duration) - (h hours)).toMinutes
//  //          val s = (duration - (h hours) - (m minutes)).toSeconds
//  //
//  //          val durationString =
//  //            s"""${
//  //              h.formatted("%d")
//  //            }:${
//  //              m.formatted("%02d")
//  //            }:${
//  //              s.formatted("%02d")
//  //            }"""
//  //
//  //          val (details, execStatus) = info match {
//  //            case f: ExecutionInfo.Failed ⇒
//  //              f.error match {
//  //                case ce: CompilationErrorData ⇒ setEditorErrors(staticInfo.now(execID).path, ce.errors)
//  //                case _                        ⇒
//  //              }
//  //              addToBanner(execID, failedDiv(execID), BannerLevel.Critical)
//  //              (ExecutionDetails("0", 0, Some(f.error), f.environmentStates), (if (!f.clean) "cleaning" else info.state))
//  //            case f: ExecutionInfo.Finished ⇒
//  //              addToBanner(execID, succesDiv(execID), BannerLevel.Regular)
//  //              (ExecutionDetails(ratio(f.completed, f.running, f.ready), f.running, envStates = f.environmentStates), (if (!f.clean) "cleaning" else info.state))
//  //            case r: ExecutionInfo.Running ⇒
//  //              setEditorErrors(staticInfo.now(execID).path, Seq())
//  //              (ExecutionDetails(ratio(r.completed, r.running, r.ready), r.running, envStates = r.environmentStates), info.state)
//  //            case c: ExecutionInfo.Canceled ⇒
//  //              hasBeenDisplayed(execID)
//  //              (ExecutionDetails("0", 0, envStates = c.environmentStates), (if (!c.clean) "cleaning" else info.state))
//  //            case r: ExecutionInfo.Compiling ⇒
//  //              (ExecutionDetails("0", 0, envStates = r.environmentStates), info.state)
//  //            case r: ExecutionInfo.Preparing ⇒ (ExecutionDetails("0", 0, envStates = r.environmentStates), info.state)
//  //          }
//  //
//  //          val srp =
//  //            SubRowPanels(
//  //              staticInfo.map { si ⇒ execTextArea(si(execID).script)(padding := 15, fontSize := "14px") },
//  //              Rx(execTextArea(outputInfo.map { oi ⇒
//  //                oi.find(_.id == execID).map {
//  //                  _.output
//  //                }.getOrElse("")
//  //              })),
//  //              Rx(execTextArea(details.error.map(ExecError.stackTrace).getOrElse(""))(padding := 15, fontSize := "14px", monospace)),
//  //              jobTable(execID).render
//  //            )
//  //
//  //          subRows() = subRows.now.updated(execID, srp)
//  //
//  //          currentSub(execID) match {
//  //            case Some(SubOutput) ⇒
//  //              expanded.now.get(execID).flatten.foreach { sub ⇒
//  //                subRowPanel(execID, srp, sub)
//  //              }
//  //            case _ ⇒
//  //          }
//  //          ReactiveRow(
//  //            execID.id,
//  //            Seq(
//  //              VarCell(span(subLink(SubScript, execID, staticInf(execID).path.name).tooltip("Original script")), 0),
//  //              VarCell(span(span(Utils.longToDate(staticInf(execID).startDate)).tooltip("Starting time")), 1),
//  //              VarCell(span(glyphAndText(glyph_flash, details.running.toString).tooltip("Running jobs")), 2),
//  //              VarCell(span(glyphAndText(glyph_flag, details.ratio.toString).tooltip("Finished/Total jobs")), 3),
//  //              VarCell(span(span(durationString).tooltip("Execution time")), 4),
//  //              VarCell(span(subLink(SubCompile, execID, execStatus, defaultModifier = executionState(info)).tooltip("Execution state")), 5),
//  //              VarCell(span(subLink(SubEnvironment, execID, "Executions").tooltip("Computation environment details")), 6),
//  //              VarCell(span(subLink(SubOutput, execID, glyphicon = OMTags.glyph_eye_open).tooltip("Hook display & standard output")), 7),
//  //              FixedCell(span(span(glyph_remove +++ ms("removeExecution"), onclick := {
//  //                () ⇒
//  //                  cancelExecution(execID)
//  //              }).tooltip("Cancel execution")), 8),
//  //              FixedCell(span(span(glyph_trash +++ ms("removeExecution"), onclick := {
//  //                () ⇒
//  //                  removeExecution(execID)
//  //              }).tooltip("Trash execution")), 9)
//  //            ))
//  //      }.toSeq
//  //    },
//  //    subRow = Some((i: scaladget.tools.ID) ⇒
//  //      SubRow(
//  //        subDiv.flatMap {
//  //          _.getOrElse(ExecutionId(i), Rx {
//  //            div("")
//  //          })
//  //        },
//  //        expanded.map {
//  //          _.get(ExecutionId(i)).flatten.isDefined
//  //        }
//  //      )
//  //    ),
//  //    bsTableStyle = BSTableStyle(tableStyle = inverse_table)
//  //  )
//  //
//  //  def setTimerOn = {
//  //    updating.set(false)
//  //    timerOn() = true
//  //  }
//  //
//  //  def setTimerOff = {
//  //    timerOn() = false
//  //  }
//  //
//  //  def updateExecutionInfo: Unit = {
//  //
//  //    def delay = {
//  //      updating.set(false)
//  //      setTimeout(5000) {
//  //        Tooltip.cleanAll
//  //        updateExecutionInfo
//  //      }
//  //    }
//  //
//  //    if (updating.compareAndSet(false, true)) {
//  //      Post()[Api].allStates(200).call().andThen {
//  //        case Success((executionInfos, runningOutputData)) ⇒
//  //          executionInfo() = executionInfos.toMap
//  //          outputInfo() = runningOutputData
//  //          if (timerOn.now) delay
//  //        case Failure(_) ⇒ delay
//  //      }
//  //    }
//  //  }
//  //
//  //  def updateStaticInfos = Post()[Api].staticInfos.call().foreach {
//  //    s ⇒
//  //      staticInfo() = s.toMap
//  //      setTimeout(0) {
//  //        updateExecutionInfo
//  //      }
//  //  }
//  //
//  //  case class ExecutionDetails(
//  //    ratio:     String,
//  //    running:   Long,
//  //    error:     Option[ExecError]     = None,
//  //    envStates: Seq[EnvironmentState] = Seq(),
//  //    outputs:   String                = ""
//  //  )
//  //
//  //  def jobTable(id: ExecutionId) = {
//  //    val jTable = new JobTable(id, this)
//  //    jobTables() = jobTables.now.updated(id, jTable)
//  //    jTable
//  //  }
//  //
//  //  def ratio(completed: Long, running: Long, ready: Long) = s"${
//  //    completed
//  //  } / ${
//  //    completed + running + ready
//  //  }"
//  //
//  //  def glyphAndText(mod: ModifierSeq, text: String) = div(
//  //    span(mod),
//  //    badge(s" $text", Seq(backgroundColor := WHITE, scalaJsDom.all.color := DARK_GREY))
//  //  )
//  //
//  //  def hasBeenDisplayed(id: ExecutionId) = executionsDisplayedInBanner() = (executionsDisplayedInBanner.now + id)
//  //
//  //  def addToBanner(id: ExecutionId, message: HtmlElement, level: BannerLevel) = {
//  //    if (!executionsDisplayedInBanner.now.contains(id)) {
//  //      bannerAlert.registerDiv(message, level)
//  //      hasBeenDisplayed(id)
//  //    }
//  //  }
//  //
//  //  def failedDiv(id: ExecutionId) = div(
//  //    s"Your simulation ${
//  //      staticInfo.now()(id).path.name
//  //    } ", a("failed", bold, color := WHITE, cursor.pointer, onClick --> {
//  //      _ ⇒
//  //        bannerAlert.clear
//  //        dialog.show
//  //    })
//  //  )
//  //
//  //  def succesDiv(id: ExecutionId) = div(
//  //    s"Your simulation ${
//  //      staticInfo.now()(id).path.name
//  //    } has ", a("finished", color := WHITE, bold, cursor.pointer, onClick --> {
//  //      _ ⇒
//  //        bannerAlert.clear
//  //        dialog.show
//  //    })
//  //  )
//  //
//  //  def cancelExecution(id: ExecutionId) = {
//  //    // setIDTabInStandBy(id)
//  //    Post()[Api].cancelExecution(id).call().foreach {
//  //      r ⇒
//  //        updateExecutionInfo
//  //    }
//  //  }
//  //
//  //  def removeExecution(id: ExecutionId) = {
//  //    Post()[Api].removeExecution(id).call().foreach {
//  //      r ⇒
//  //        updateExecutionInfo
//  //    }
//  //    executionsDisplayedInBanner.set(executionsDisplayedInBanner.now - id)
//  //  }
//  //
//  val dialogHeader = div(height := "55", b("Executions"))
//
//  val dialogBody = div(cls := "executionTable", executionTable)
//
//  val dialogFooter = closeButton("Close", () ⇒ dialog.hide)
//
//  lazy val dialog: ModalDialog = ModalDialog(
//    dialogHeader,
//    dialogBody,
//    dialogFooter,
//    omsheet.panelWidth(92),
//    onopen = () ⇒ {
//      // setTimerOn
//      // updateStaticInfos
//    },
//    onclose = () ⇒ {
//      // setTimerOff
//      //  Tooltip.cleanAll
//    }
//  )
//
//}