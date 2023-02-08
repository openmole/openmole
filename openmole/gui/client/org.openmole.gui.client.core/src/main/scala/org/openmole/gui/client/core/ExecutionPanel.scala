package org.openmole.gui.client.core

import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Failure, Success}
import org.openmole.gui.client.ext.*

import scala.scalajs.js.timers.*
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.data.ErrorData as ExecError
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.core.alert.{BannerAlert, BannerLevel}
import org.openmole.gui.client.core.files.{OMSContent, TabContent, TreeNodeTabs}
import org.openmole.gui.client.tool.{Component, OMTags}
import org.openmole.gui.shared.data.ExecutionState.Failed
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.Panels.ExpandablePanel
import org.openmole.gui.client.ext.Utils
import org.openmole.gui.shared.api.ServerAPI

import concurrent.duration.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*

object ExecutionPanel:
  object ExecutionDetails:
    object State:
      def apply(info: ExecutionState) =
        info match
          case f: ExecutionState.Failed => State.failed(!f.clean)
          case _: ExecutionState.Running => State.running
          case f: ExecutionState.Canceled => State.canceled(!f.clean)
          case f: ExecutionState.Finished => State.completed(!f.clean)
          case _: ExecutionState.Preparing => State.preparing

      def toString(s: State) =
        s match
          case State.preparing => "preparing"
          case State.running => "running"
          case State.completed(true) => "cleaning"
          case State.completed(false) => "completed"
          case State.failed(true) => "cleaning"
          case State.failed(false) => "failed"
          case State.canceled(true) => "cleaning"
          case State.canceled(false) => "canceled"

      def isFailedOrCanceled(state: State) =
        state match
          case (_: State.canceled) | (_: State.failed) => true
          case _ => false

    enum State:
      case preparing, running
      case completed(cleaning: Boolean) extends State
      case failed(cleaning: Boolean) extends State
      case canceled(cleaning: Boolean) extends State

  case class ExecutionDetails(
                               path: SafePath,
                               script: String,
                               state: ExecutionDetails.State,
                               startDate: Long,
                               duration: Long,
                               ratio: String,
                               running: Long,
                               error: Option[ExecError] = None,
                               envStates: Seq[EnvironmentState] = Seq(),
                               output: String = "")

  //  type Statics = Map[ExecutionId, StaticExecutionInfo]
  type Execs = Map[ExecutionId, ExecutionDetails]

  def open(using api: ServerAPI, panels: Panels) =
    Panels.expandTo(panels.executionPanel.render, 4)

class ExecutionPanel:

  import ExecutionPanel.*

  //  val staticInfos: Var[ExecutionPanel.Statics] = Var(Map())
  //  val outputInfos: Var[Seq[OutputStreamData]] = Var(Seq())
  //val timerOn = Var(false)
  val currentOpenSimulation: Var[Option[ExecutionId]] = Var(None)


  //  def setTimerOn = {
  //    updating.set(false)
  //    timerOn.set(true)
  //  }

  //def setTimerOff = timerOn.set(false)

  def toExecDetails(exec: ExecutionData): ExecutionDetails =
    import ExecutionPanel.ExecutionDetails.State
    exec.state match
      case f: ExecutionState.Failed ⇒ ExecutionDetails(exec.path, exec.script, State(exec.state), exec.startDate, exec.duration, "0", 0, Some(f.error), f.environmentStates, exec.output)
      case f: ExecutionState.Finished ⇒ ExecutionDetails(exec.path, exec.script, State(exec.state), exec.startDate, exec.duration, ratio(f.completed, f.running, f.ready), f.running, envStates = f.environmentStates, exec.output)
      case r: ExecutionState.Running ⇒ ExecutionDetails(exec.path, exec.script, State(exec.state), exec.startDate, exec.duration, ratio(r.completed, r.running, r.ready), r.running, envStates = r.environmentStates, exec.output)
      case c: ExecutionState.Canceled ⇒ ExecutionDetails(exec.path, exec.script, State(exec.state), exec.startDate, exec.duration, "0", 0, envStates = c.environmentStates, exec.output)
      case r: ExecutionState.Preparing ⇒ ExecutionDetails(exec.path, exec.script, State(exec.state), exec.startDate, exec.duration, "0", 0, envStates = r.environmentStates, exec.output)


  def updateScriptError(path: SafePath, details: ExecutionDetails)(using panels: Panels) = OMSContent.setError(path, details.error)


  val rowFlex = Seq(display.flex, flexDirection.row, alignItems.center)
  val columnFlex = Seq(display.flex, flexDirection.column, justifyContent.flexStart)

  enum Expand:
    case Console, Script, ErrorLog, Computing


  val showDurationOnCores = Var(false)
  val showExpander: Var[Option[Expand]] = Var(None)
  val showControls = Var(false)

  def contextBlock(info: String, content: String, alwaysOpaque: Boolean = false) =
    div(columnFlex,
      div(cls := "contextBlock",
        if (!alwaysOpaque) backgroundOpacityCls else emptyMod,
        div(info, cls := "info"), div(content, cls := "infoContent"))
    )

  def statusBlock(info: String, content: String) =
    statusBlockFromDiv(info, div(content, cls := "infoContent"), "statusBlock")


  def statusBlockFromDiv(info: String, contentDiv: Div, blockCls: String) =
    div(columnFlex, div(cls := blockCls, div(info, cls := "info"), contentDiv,backgroundOpacityCls))

  def durationBlock(simpleTime: Long, timeOnCores: Long) =

    val duration: Duration = (simpleTime milliseconds)
    val h = (duration).toHours
    val m = ((duration) - (h hours)).toMinutes
    val s = (duration - (h hours) - (m minutes)).toSeconds

    val durationString =
      s"""${
        "%d".format(h)
      }:${
        "%02d".format(m)
      }:${
        "%02d".format(s)
      }"""

    div(columnFlex, div(cls := "statusBlock",
      div(child <-- showDurationOnCores.signal.map { d => if (d) "Duration on cores" else "Duration" }, cls := "info"),
      div(child <-- showDurationOnCores.signal.map { d => if (d) "??" else durationString }, cls := "infoContentLink")),
      backgroundOpacityCls,
      onClick --> { _ => showDurationOnCores.update(!_) },
      cursor.pointer
    )

  def backgroundOpacityCls = cls.toggle("silentBlock") <-- showExpander.signal.map {_ != None}

  def backgroundOpacityCls(expand: Expand) =
    cls.toggle("silentBlock") <-- showExpander.signal.map {se=> se != Some(expand) && se != None}

  def showHideBlock(expand: Expand, title: String, messageWhenClosed: String, messageWhenOpen: String) =
    div(columnFlex, div(cls := "statusBlock",
      backgroundOpacityCls(expand),
      div(title, cls := "info"),
      div(child <-- showExpander.signal.map { c =>
        if (c == Some(expand)) messageWhenOpen else messageWhenClosed
      }, cls := "infoContentLink")),
      onClick --> { _ =>
        showExpander.update(exp =>
          if (exp == Some(expand)) None
          else Some(expand)
        )
      },
      cursor.pointer
    )

  def simulationStatusBlock(state: ExecutionDetails.State) =
    div(columnFlex, div(cls := "statusBlockNoColor",
      div("Status", cls := "info"),
      div(ExecutionDetails.State.toString(state).capitalize, cls := {
        if (state == ExecutionDetails.State.failed) "infoContentLink"
        else "infoContent"
      }),
      onClick --> { _ =>
        showExpander.update(exp =>
          if (exp == Some(Expand.ErrorLog)) None
          else Some(Expand.ErrorLog)
        )
      },
      cursor.pointer
    )
    )


  def controls(id: ExecutionId, cancel: ExecutionId => Unit, remove: ExecutionId => Unit) = div(cls := "execButtons",
    child <-- showControls.signal.map { c =>
      if (c)
        div(display.flex, flexDirection.column, alignItems.center,
          button("Stop", onClick --> { _ => cancel(id) }, btn_danger, cls := "controlButton"),
          button("Clean", onClick --> { _ => remove(id) }, btn_secondary, cls := "controlButton"),
        )
      else div()
    }
  )

  def ratio(completed: Long, running: Long, ready: Long) = s"${
    completed
  } / ${
    completed + running + ready
  }"

  def executionRow(id: ExecutionId, details: ExecutionDetails, cancel: ExecutionId => Unit, remove: ExecutionId => Unit) =
    div(rowFlex, justifyContent.center,
      showHideBlock(Expand.Script, "Script", details.path.name, details.path.name),
      contextBlock("Start time", Utils.longToDate(details.startDate)),
      contextBlock("Method", "???"),
      durationBlock(details.duration, 0L),
      statusBlock("Running", details.running.toString),
      statusBlock("Completed", details.ratio),
      simulationStatusBlock(details.state).amend(backgroundColor := statusColor(details.state), backgroundOpacityCls(Expand.ErrorLog)),
      showHideBlock(Expand.Console, "Standard output", "Show", "Hide"),
      showHideBlock(Expand.Computing, "Computing", "Show", "Hide"),
      div(cls := "bi-three-dots-vertical execControls", onClick --> { _ => showControls.update(!_) }),
      controls(id, cancel, remove)
    )

  def jobs(envStates: Seq[EnvironmentState]) =
    div(columnFlex, marginTop := "20px",
      for
        e <- envStates
      yield jobRow(e)
    )

  private def displaySize(size: Long, readable: String, operations: Int) =
    if (size > 0) s"$operations ($readable)" else s"$operations"

  val openEnvironmentErrors: Var[Option[EnvironmentId]] = Var(None)

  def jobRow(e: EnvironmentState) =
    div(columnFlex,
      div(rowFlex, justifyContent.center,
        contextBlock("Resource", e.taskName, true).amend(width := "180"),
        contextBlock("Execution time", CoreUtils.approximatedYearMonthDay(e.executionActivity.executionTime), true),
        contextBlock("Uploads", displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize, e.networkActivity.uploadingFiles), true),
        contextBlock("Downloads", displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize, e.networkActivity.uploadingFiles), true),
        contextBlock("Submitted", e.submitted.toString, true),
        contextBlock("Running", e.running.toString, true),
        contextBlock("Finished", e.done.toString, true),
        contextBlock("Failed", e.failed.toString, true),
        contextBlock("Errors", e.numberOfErrors.toString, true).amend(onClick --> { _ =>
          openEnvironmentErrors.update(id =>
            if id == Some(e.envId)
            then None
            else Some(e.envId)
          )
        }, cursor.pointer),
      ),
      openEnvironmentErrors.signal.map(eID => eID == Some(e.envId)).expand(div("Errors", width := "100%", height := "200px", backgroundColor := "pink"))
    )

  //def evironmentErrors(environmentError: EnvironmentError)

  def execTextArea(content: String): HtmlElement = textArea(content, idAttr := "execTextArea")

  def expander(details: ExecutionDetails) =
    div(height := "500", rowFlex, justifyContent.center, alignItems.flexStart,
      child <-- showExpander.signal.map {
        _ match
          case Some(Expand.Script) => div(execTextArea(details.script))
          case Some(Expand.Console) => div(execTextArea(details.output).amend(cls := "console"))
          case Some(Expand.ErrorLog) => div(execTextArea(details.error.map(ExecError.stackTrace).getOrElse("")))
          case Some(Expand.Computing) => jobs(details.envStates)
          case None => div()
      }
    )

  def buildExecution(id: ExecutionId, executionDetails: ExecutionDetails, cancel: ExecutionId => Unit, remove: ExecutionId => Unit)(using panels: Panels) =
    OMSContent.setError(executionDetails.path, executionDetails.error)
    elementTable()
      .addRow(executionRow(id, executionDetails, cancel, remove)).expandTo(expander(executionDetails), showExpander.signal.map(_.isDefined))
      .unshowSelection
      .render.render.amend(idAttr := "exec")

  def statusColor(status: ExecutionPanel.ExecutionDetails.State) =
    import ExecutionPanel.ExecutionDetails.State
    status match
      case State.completed(_) => "#00810a"
      case State.failed(_) => "#c8102e"
      case State.canceled(_) => "#d14905"
      case State.preparing => "#f1c306"
      case State.running => "#a5be21"


  def simulationBlock(executionId: ExecutionId, executionInfo: ExecutionDetails) =
    div(rowFlex, justifyContent.center, alignItems.center,
      cls := "simulationInfo",
      cls.toggle("statusOpenSim") <-- currentOpenSimulation.signal.map { os => os == Some(executionId) },
      div("", cls := "simulationID", backgroundColor := statusColor(executionInfo.state)),
      div(executionInfo.path.nameWithNoExtension),
      cursor.pointer,
      onClick --> { _ =>
        currentOpenSimulation.update {
          _ match {
            case None => Some(executionId)
            case Some(x: ExecutionId) if (x != executionId) => Some(executionId)
            case _ => None
          }
        }
      }
    )

  lazy val autoRemoveFailed = Component.Switch("auto remove failed", true, "autoCleanExecSwitch")

  def render(using panels: Panels, api: ServerAPI) =
    def filterExecutions(execs: Execs): (Execs, Seq[ExecutionId]) =
      import ExecutionPanel.ExecutionDetails.State
      val (ids, cleanIds) =
        if autoRemoveFailed.isChecked
        then
          val idsForPath =
            execs.groupBy(_._2.path).toSeq
              .map { (k, v) => k -> v.toSeq.sortBy(x => x._2.startDate) }
              .map { (_, t) => t.map(_._1) }

          idsForPath.foldLeft((Seq[ExecutionId](), Seq[ExecutionId]())) { (s, execIds) =>
            val (failedOrCanceled, otherStates) = execIds.partition(i => State.isFailedOrCanceled(execs(i).state))
            val (toBeCleaned, toBeKept) = (failedOrCanceled.dropRight(1), failedOrCanceled.takeRight(1))
            (s._1 ++ otherStates ++ toBeKept, s._2 ++ toBeCleaned)
          }
        else (execs.map(_._1).toSeq, Seq())

      (ids.map(id => id -> execs(id)).toMap, cleanIds)


    val forceUpdate = Var(0)
    @volatile var queryingState = false

    def queryState =
      queryingState = true
      try
        for executionData <- api.executionState(200)
          yield executionData.map { e => e.id -> toExecDetails(e) }.toMap
      finally queryingState = false

    def delay(milliseconds: Int): scala.concurrent.Future[Unit] =
      val p = scala.concurrent.Promise[Unit]()
      setTimeout(milliseconds) {
        p.success(())
      }
      p.future


    val initialDelay = Signal.fromFuture(delay(1000))
    val periodicUpdate = EventStream.periodic(10000, emitInitial = false).filter(_ => !queryingState).toSignal(0)

    div(
      columnFlex, width := "100%", marginTop := "20",
      children <--
        (initialDelay combineWith periodicUpdate combineWith forceUpdate.signal combineWith currentOpenSimulation.signal).flatMap { (_, _, _, id) =>
          EventStream.fromFuture(queryState).map { allDetails =>
            val (details, toClean) = filterExecutions(allDetails)
            toClean.foreach(api.removeExecution)
            Seq(
              div(rowFlex, justifyContent.center,
                details.toSeq.sortBy(_._2.startDate).reverse.map { (id, detailValue) => simulationBlock(id, detailValue) }
              ),
              autoRemoveFailed.element,
              div(
                id.map { idValue =>
                  details.get(idValue) match
                    case Some(st) =>
                      def cancel(id: ExecutionId) = api.cancelExecution(id).andThen { case Success(_) => forceUpdate.update(_ + 1) }

                      def remove(id: ExecutionId) = api.removeExecution(id).andThen { case Success(_) => forceUpdate.update(_ + 1) }

                      div(buildExecution(idValue, st, cancel, remove))
                    case None => div()
                }
              )
            )
          }
        }
    )


//lazy val executionTable = scaladget.bootstrapnative.Table(
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



