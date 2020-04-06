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
import scalatags.JsDom._
import org.openmole.gui.ext.tool.client._

import scala.scalajs.js.timers._
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import org.openmole.gui.ext.data.{ ErrorData ⇒ ExecError }
import org.openmole.gui.ext.data._
import org.openmole.gui.client.core.alert.BannerAlert
import org.openmole.gui.client.core.alert.BannerAlert.BannerMessage
import org.openmole.gui.client.core.files.TreeNodeTabs
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.ExecutionInfo.Failed
import org.openmole.gui.ext.tool.client.Utils
import org.scalajs.dom.raw.{ HTMLElement, HTMLSpanElement }
import rx._
import scaladget.bootstrapnative.Table.{ BSTableStyle, FixedCell, ReactiveRow, SubRow, VarCell }

import concurrent.duration._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import scaladget.bootstrapnative.bsn.ScrollableTextArea.BottomScroll

object ExecutionPanel {

  sealed trait JobView

  object CapsuleView extends JobView

  object EnvironmentView extends JobView

  sealed trait Sub

  object SubScript extends Sub

  object SubOutput extends Sub

  object SubCompile extends Sub

  object SubEnvironment extends Sub

  implicit def idToExecutionID(id: scaladget.tools.ID): ExecutionId = ExecutionId(id)

}

import ExecutionPanel._

class ExecutionPanel {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  case class ExInfo(id: ExecutionId, info: Var[ExecutionInfo])

  val staticInfo: Var[Map[ExecutionId, StaticExecutionInfo]] = Var(Map())
  val executionInfo: Var[Map[ExecutionId, ExecutionInfo]] = Var(Map())

  val outputInfo: Var[Seq[OutputStreamData]] = Var(Seq())
  val jobTables: Var[Map[ExecutionId, JobTable]] = Var(Map())

  val executionsDisplayedInBanner: Var[Set[ExecutionId]] = Var(Set())

  val timerOn = Var(false)

  val updating = new AtomicBoolean(false)

  case class SubRowPanels(
    script:      Rx[TypedTag[HTMLElement]],
    output:      Rx[TypedTag[HTMLElement]],
    failedStack: Rx[TypedTag[HTMLElement]],
    environment: Rx[TypedTag[HTMLElement]])

  val emptySubRowPanel = SubRowPanels(Rx(tags.div("")), Rx(tags.div("")), Rx(tags.div("")), Rx(tags.div("")))

  val subRows: Var[Map[ExecutionId, SubRowPanels]] = Var(Map())
  val expanded: Var[Map[ExecutionId, Option[Sub]]] = Var(Map())
  val subDiv: Var[Map[ExecutionId, Rx[TypedTag[HTMLElement]]]] = Var(Map())

  def subRowPanel(executionId: ExecutionId, srp: SubRowPanels, sub: Sub) = {
    subDiv.update(subDiv.now.updated(
      executionId,
      sub match {
        case SubScript      ⇒ srp.script
        case SubOutput      ⇒ srp.output
        case SubCompile     ⇒ srp.failedStack
        case SubEnvironment ⇒ srp.environment
      }

    ))
  }

  def currentSub(id: ExecutionId) = expanded.now.get(id).flatten

  def subLink(s: Sub, id: ExecutionId, name: String = "", glyphicon: Glyphicon = emptyMod, defaultModifier: ModifierSeq = Seq(scalatags.JsDom.all.color := WHITE), selectedModifier: ModifierSeq = Seq(scalatags.JsDom.all.color := BLUE, fontWeight := "bold")) =
    tags.span(Rx {
      tags.span(glyphicon, pointer,
        {
          if (expanded().get(id).flatten == Some(s)) selectedModifier
          else defaultModifier
        },
        onclick := { () ⇒
          subRows.now.get(id).foreach { srp ⇒
            subRowPanel(id, srp, s)
          }

          expanded() = expanded.now.updated(id, currentSub(id) match {
            case Some(ss: Sub) ⇒
              if (ss == s) None
              else Some(s)
            case _ ⇒ Some(s)
          })

        })(name)
    }
    )

  def execTextArea(content: String): TypedTag[HTMLElement] = textarea(content, height := "300px", width := "100%", scalatags.JsDom.all.color := "#222")

  def execTextArea(content: Rx[String]): TypedTag[HTMLElement] = {
    val st = scrollableText(content.now, BottomScroll)
    content.trigger {
      st.setContent(content.now)
      st.doScroll
    }
    div(st.sRender)
  }

  lazy val executionTable = scaladget.bootstrapnative.Table(
    for {
      execMap ← executionInfo
      staticInf ← staticInfo
    } yield {
      execMap.toSeq.sortBy(e ⇒ staticInf(e._1).startDate).map {
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

          val (details, execStatus) = info match {
            case f: ExecutionInfo.Failed ⇒
              panels.treeNodeTabs.find(staticInfo.now(execID).path).foreach { tab ⇒
                f.error match {
                  case ce: CompilationErrorData ⇒ tab.editor.foreach { _.setErrors(ce.errors) }
                  case _                        ⇒
                }
              }
              addToBanner(execID, BannerAlert.div(failedDiv(execID)).critical)
              (ExecutionDetails("0", 0, Some(f.error), f.environmentStates), (if (!f.clean) "cleaning" else info.state))
            case f: ExecutionInfo.Finished ⇒
              addToBanner(execID, BannerAlert.div(succesDiv(execID)))
              (ExecutionDetails(ratio(f.completed, f.running, f.ready), f.running, envStates = f.environmentStates), (if (!f.clean) "cleaning" else info.state))
            case r: ExecutionInfo.Running ⇒
              panels.treeNodeTabs.find(staticInfo.now(execID).path).foreach { tab ⇒ tab.editor.foreach { _.setErrors(Seq()) } }
              (ExecutionDetails(ratio(r.completed, r.running, r.ready), r.running, envStates = r.environmentStates), info.state)
            case c: ExecutionInfo.Canceled ⇒
              hasBeenDisplayed(execID)
              (ExecutionDetails("0", 0, envStates = c.environmentStates), (if (!c.clean) "cleaning" else info.state))
            case r: ExecutionInfo.Compiling ⇒
              (ExecutionDetails("0", 0, envStates = r.environmentStates), info.state)
            case r: ExecutionInfo.Preparing ⇒ (ExecutionDetails("0", 0, envStates = r.environmentStates), info.state)
          }

          val srp =
            SubRowPanels(
              staticInfo.map { si ⇒ execTextArea(si(execID).script)(padding := 15, fontSize := "14px") },
              Rx(execTextArea(outputInfo.map { oi ⇒ oi.find(_.id == execID).map { _.output }.getOrElse("") })),
              Rx(execTextArea(details.error.map(ExecError.stackTrace).getOrElse(""))(padding := 15, fontSize := "14px", monospace)),
              jobTable(execID).render
            )

          subRows() = subRows.now.updated(execID, srp)

          currentSub(execID) match {
            case Some(SubOutput) ⇒
              expanded.now.get(execID).flatten.foreach { sub ⇒
                subRowPanel(execID, srp, sub)
              }
            case _ ⇒
          }
          ReactiveRow(
            execID.id,
            Seq(
              VarCell(tags.span(subLink(SubScript, execID, staticInf(execID).path.name).tooltip("Original script")), 0),
              VarCell(tags.span(tags.span(Utils.longToDate(staticInf(execID).startDate)).tooltip("Starting time")), 1),
              VarCell(tags.span(glyphAndText(glyph_flash, details.running.toString).tooltip("Running jobs")), 2),
              VarCell(tags.span(glyphAndText(glyph_flag, details.ratio.toString).tooltip("Finished/Total jobs")), 3),
              VarCell(tags.span(tags.span(durationString).tooltip("Execution time")), 4),
              VarCell(tags.span(subLink(SubCompile, execID, execStatus, defaultModifier = executionState(info)).tooltip("Execution state")), 5),
              VarCell(tags.span(subLink(SubEnvironment, execID, "Executions").tooltip("Computation environment details")), 6),
              VarCell(tags.span(subLink(SubOutput, execID, glyphicon = OMTags.glyph_eye_open).tooltip("Hook display & standard output")), 7),
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
        subDiv.flatMap {
          _.getOrElse(ExecutionId(i), Rx {
            div("")
          })
        },
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
      post()[Api].allStates(200).call().andThen {
        case Success((executionInfos, runningOutputData)) ⇒
          executionInfo() = executionInfos.toMap
          outputInfo() = runningOutputData
          if (timerOn.now) delay
        case Failure(_) ⇒ delay
      }
    }
  }

  def updateStaticInfos = post()[Api].staticInfos.call().foreach {
    s ⇒
      staticInfo() = s.toMap
      setTimeout(0) {
        updateExecutionInfo
      }
  }

  case class ExecutionDetails(
    ratio:     String,
    running:   Long,
    error:     Option[ExecError]     = None,
    envStates: Seq[EnvironmentState] = Seq(),
    outputs:   String                = ""
  )

  def jobTable(id: ExecutionId) = {
    val jTable = JobTable(id)
    jobTables() = jobTables.now.updated(id, jTable)
    jTable
  }

  def ratio(completed: Long, running: Long, ready: Long) = s"${
    completed
  } / ${
    completed + running + ready
  }"

  def glyphAndText(mod: ModifierSeq, text: String) = tags.div(
    tags.span(mod),
    badge(s" $text", Seq(backgroundColor := WHITE, scalatags.JsDom.all.color := DARK_GREY))
  )

  def hasBeenDisplayed(id: ExecutionId) = executionsDisplayedInBanner() = (executionsDisplayedInBanner.now + id)

  def addToBanner(id: ExecutionId, bannerMessage: BannerMessage) = {
    if (!executionsDisplayedInBanner.now.contains(id)) {
      BannerAlert.register(bannerMessage)
      hasBeenDisplayed(id)
    }
  }

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

  def cancelExecution(id: ExecutionId) = {
    // setIDTabInStandBy(id)
    post()[Api].cancelExecution(id).call().foreach {
      r ⇒
        updateExecutionInfo
    }
  }

  def removeExecution(id: ExecutionId) = {
    post()[Api].removeExecution(id).call().foreach {
      r ⇒
        updateExecutionInfo
    }
    executionsDisplayedInBanner() = executionsDisplayedInBanner.now - id
  }

  val dialog = ModalDialog(
    omsheet.panelWidth(92),
    onopen = () ⇒ {
      setTimerOn
      updateStaticInfos
    },
    onclose = () ⇒ {
      setTimerOff
      Tooltip.cleanAll
    }
  )

  dialog.header(
    div(height := 55)(
      b("Executions")
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