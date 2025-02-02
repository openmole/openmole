package org.openmole.gui.client.core

import com.raquo.laminar.DomApi

import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Failure, Success}
import org.openmole.gui.client.ext.*

import scala.scalajs.js.timers.*
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.data.ErrorData
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.core.files.{OMSContent, TabContent}
import org.openmole.gui.client.tool.{Component, OMTags}
import org.openmole.gui.shared.data.ExecutionState.{CapsuleExecution, Failed}
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.api.features.unitArrows
import com.raquo.laminar.api.Laminar
import com.raquo.laminar.nodes.ReactiveElement
import org.openmole.gui.client.core.Panels.ExpandablePanel
import org.openmole.gui.client.ext.ClientUtil
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.ErrorData.stackTrace
import org.scalajs.dom.HTMLDivElement

import concurrent.duration.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*

import scala.concurrent.Future

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
          case _: State.canceled | _: State.failed => true
          case _ => false

      def isFailed(state: State) =
        state match
          case _: State.failed => true
          case _ => false

      def isFinished(state: State) =
        state == State.failed(false) || state == State.completed(false) || state == State.canceled(false)


    enum State:
      case running
      case preparing extends State
      case completed(cleaning: Boolean) extends State
      case failed(cleaning: Boolean) extends State
      case canceled(cleaning: Boolean) extends State

  case class ExecutionDetails(
    path: SafePath,
    script: String,
    state: ExecutionDetails.State,
    startDate: Long,
    duration: Long,
    executionTime: Long,
    ratio: String,
    running: Long,
    error: Option[ErrorData] = None,
    envStates: Seq[EnvironmentState] = Seq())

  //  type Statics = Map[ExecutionId, StaticExecutionInfo]
  type Executions = Map[ExecutionId, ExecutionDetails]

  def open(using api: ServerAPI, path: BasePath, panels: Panels) =
    panels.expandTo(panels.executionPanel.render, 4)

  enum Expand:
    case Console, Script, ErrorLog, Computing


class ExecutionPanel:

  import ExecutionPanel.*

  val currentOpenSimulation: Var[Option[ExecutionId]] = Var(None)

  val rowFlex = Seq(display.flex, flexDirection.row, alignItems.center)
  val columnFlex = Seq(display.flex, flexDirection.column, justifyContent.flexStart)

  val showDurationOnCores = Var(false)
  val showExpander: Var[Option[Expand]] = Var(None)
  val showControls = Var(false)
  val showGlobalControls = Var(false)
  val showEvironmentControls = Var(false)
  val details: Var[Executions] = Var(Map())

  def toExecDetails(exec: ExecutionData, panels: Panels): ExecutionDetails =
    import ExecutionPanel.ExecutionDetails.State
    def userCapsuleState(c: Seq[CapsuleExecution]) =
      val ready = c.map(c => c.statuses.ready * c.userCardinality).sum
      val running = c.map(c => c.statuses.running * c.userCardinality).sum
      val completed = c.map(c => c.statuses.completed * c.userCardinality).sum
      (ready, running, completed)

    exec.state match
      case f: ExecutionState.Failed ⇒ ExecutionDetails(exec.path, exec.script, State(exec.state), exec.startDate, exec.duration, exec.executionTime, "0", 0, Some(f.error), f.environmentStates)
      case f: ExecutionState.Finished ⇒
        val (ready, running, completed) = userCapsuleState(f.capsules)
        ExecutionDetails(exec.path, exec.script, State(exec.state), exec.startDate, exec.duration, exec.executionTime, ratio(completed, running, ready), running, envStates = f.environmentStates)
      case r: ExecutionState.Running ⇒
        val (ready, running, completed) = userCapsuleState(r.capsules)
        ExecutionDetails(exec.path, exec.script, State(exec.state), exec.startDate, exec.duration, exec.executionTime, ratio(completed, running, ready), running, envStates = r.environmentStates)
      case c: ExecutionState.Canceled ⇒ ExecutionDetails(exec.path, exec.script, State(exec.state), exec.startDate, exec.duration, exec.executionTime, "0", 0, envStates = c.environmentStates)
      case r: ExecutionState.Preparing ⇒ ExecutionDetails(exec.path, exec.script, State(exec.state), exec.startDate, exec.duration, exec.executionTime, "0", 0, envStates = r.environmentStates)


  //def updateScriptError(path: SafePath, details: ExecutionDetails)(using panels: Panels) = OMSContent.setError(path, details.error)


  def contextBlock(info: String, content: String, alwaysOpaque: Boolean = false, link: Boolean = false, blockWidth: Int = 150) =
    div(columnFlex,
      div(cls := "contextBlock", width := blockWidth.toString,
        if (!alwaysOpaque) backgroundOpacityCls else emptyMod,
        div(info, cls := "info"), div(content, cls := (if !link then "infoContent" else "infoContentLink")))
    )

  def statusBlock(info: String, content: String) =
    statusBlockFromDiv(info, div(content, cls := "infoContent"), "statusBlock")


  def statusBlockFromDiv(info: String, contentDiv: Div, blockCls: String) =
    div(columnFlex, div(cls := blockCls, div(info, cls := "info"), contentDiv, backgroundOpacityCls))

  def timeToString(simpleTime: Long) =
    val duration: Duration = (simpleTime milliseconds)
    val h = (duration).toHours
    val m = ((duration) - (h hours)).toMinutes
    val s = (duration - (h hours) - (m minutes)).toSeconds

    s"""${
      "%d".format(h)
    }:${
      "%02d".format(m)
    }:${
      "%02d".format(s)
    }"""

  def durationBlock(simpleTime: Long, executionTime: Long) =
    div(columnFlex, div(cls := "statusBlock",
      div(child <-- showDurationOnCores.signal.map { d => if d then "Execution Time" else "Duration" }, cls := "info"),
      div(child <-- showDurationOnCores.signal.map { d => if d then timeToString(executionTime) else timeToString(simpleTime) }, cls := "infoContentLink")),
      backgroundOpacityCls,
      onClick --> { _ => showDurationOnCores.update(!_) },
      cursor.pointer
    )

  def backgroundOpacityCls =
    cls.toggle("silentBlock") <-- showExpander.signal.map { _.isDefined }

  def backgroundOpacityCls(expand: Expand) =
    cls.toggle("silentBlock") <-- showExpander.signal.map { se => !se.contains(expand) && se.isDefined }

  def showHideBlock(expand: Expand, title: String, messageWhenClosed: String, messageWhenOpen: String) =
    div(columnFlex,
      div(cls := "statusBlock", backgroundOpacityCls(expand),
        div(title, cls := "info"),
        div(child <--
          showExpander.signal.map:
            case Some(_) => messageWhenOpen
            case None => messageWhenClosed
          ,
          cls := "infoContentLink"
        )
      ),
      onClick -->
        showExpander.update:
          case Some(e) if e == expand => None
          case _ => Some(expand)
      ,
      cursor.pointer
    )

  def simulationStatusBlock(state: ExecutionDetails.State) =
    div(columnFlex,
      div(cls := "statusBlockNoColor",
        div("Status", cls := "info"),
        div(
          ExecutionDetails.State.toString(state).capitalize,
          cls := (state match
            case ExecutionDetails.State.failed(_) => "infoContentLink"
            case _ => "infoContent")
        ),
        state match
          case ExecutionDetails.State.failed(_) =>
            onClick -->
              showExpander.update:
                case Some(Expand.ErrorLog) => None
                case _ => Some(Expand.ErrorLog)
          case _ => emptyMod
        ,
        state match
          case ExecutionDetails.State.failed(_) => cursor.pointer
          case _ => emptyMod
      )
    )

  def controls(id: ExecutionId, state: ExecutionDetails.State, cancel: ExecutionId => Unit, remove: ExecutionId => Unit) =
     div(
      child <-- showControls.signal.map: c =>
        if c
        then
          div(display.flex, flexDirection.column, alignItems.center,
            button("Stop", onClick --> cancel(id), btn_danger, cls := "controlButton"),
            button("Clean", onClick --> remove(id), btn_secondary, cls := "controlButton"),
          )
        else div()
  )

  def ratio(completed: Long, running: Long, ready: Long) = s"${
    completed
  } / ${
    completed + running + ready
  }"

  def executionRow(id: ExecutionId, details: ExecutionDetails, cancel: ExecutionId => Unit, remove: ExecutionId => Unit) =
    div(rowFlex, justifyContent.center,
      showHideBlock(Expand.Script, "Script", details.path.name, details.path.name),
      contextBlock("Start time", ClientUtil.longToDate(details.startDate), blockWidth = 225),
      //contextBlock("Method", "???"),
      durationBlock(details.duration, details.executionTime),
      statusBlock("Running", details.running.toString),
      statusBlock("Completed", details.ratio),
      simulationStatusBlock(details.state).amend(backgroundColor := statusColor(details.state), backgroundOpacityCls(Expand.ErrorLog)),
      showHideBlock(Expand.Console, "Standard output", "Show", "Hide"),
      showHideBlock(Expand.Computing, "Computing", "Show", "Hide"),
      div(cls := "bi-three-dots-vertical execControls", onClick --> { _ => showControls.update(!_) }),
      controls(id, details.state, cancel, remove)
    )

  private def displaySize(size: Long, readable: String, operations: Int) =
    if (size > 0) s"$operations ($readable)" else s"$operations"

  def execTextArea(content: String) = textArea(content, cls := "execTextArea", fontFamily := "monospace", fontSize := "medium")

  def statusColor(status: ExecutionPanel.ExecutionDetails.State) =
    import ExecutionPanel.ExecutionDetails.State
    status match
      case State.completed(_) => "#00810a"
      case State.failed(_) => "#c8102e"
      case State.canceled(_) => "#d14905"
      case State.preparing => "#f1c345"
      case State.running => "#a5be21"


  def simulationBlock(executionId: ExecutionId, executionInfo: ExecutionDetails) =
    div(rowFlex, justifyContent.center, alignItems.center,
      cls := "simulationInfo",
      cls.toggle("statusOpenSim") <-- currentOpenSimulation.signal.map { os => os == Some(executionId) },
      div("", cls := "simulationID", backgroundColor := statusColor(executionInfo.state)),
      div(executionInfo.path.nameWithoutExtension),
      cursor.pointer,
      onClick -->
        currentOpenSimulation.update:
          case None => Some(executionId)
          case Some(x: ExecutionId) if x != executionId => Some(executionId)
          case _ => None
    )

  lazy val autoRemoveFailed = Component.Switch("remove failed", true)

  def render(using panels: Panels, api: ServerAPI, path: BasePath) =
    def filterExecutions(execs: Executions): (Executions, Seq[ExecutionId]) =
      import ExecutionPanel.ExecutionDetails.State
      val (ids, cleanIds) =
        if autoRemoveFailed.checked.now()
        then
          val idsForPath =
            execs.groupBy(_._2.path).toSeq
              .map { (k, v) => k -> v.toSeq.sortBy(x => x._2.startDate) }
              .map { (_, t) => t.map(_._1) }

          idsForPath.foldLeft((Seq[ExecutionId](), Seq[ExecutionId]())): (s, execIds) =>
            val (failedOrCanceled, otherStates) = execIds.partition(i => State.isFailedOrCanceled(execs(i).state))
            val (toBeCleaned, toBeKept) = (failedOrCanceled.dropRight(1), failedOrCanceled.takeRight(1))
            (s._1 ++ otherStates ++ toBeKept, s._2 ++ toBeCleaned)

        else (execs.map(_._1).toSeq, Seq())

      (ids.map(id => id -> execs(id)).toMap, cleanIds)


    @volatile var queryingState = false
    def queryState =
      queryingState = true
      try for executionData <- api.executionState() yield executionData.map { e => e.id -> toExecDetails(e, panels) }.toMap
      finally queryingState = false

    def jobs(executionId: ExecutionId, envStates: Seq[EnvironmentState]) =
      div(columnFlex, marginTop := "5px", maxWidth := "1520px",
        for
          e <- envStates
        yield jobRow(executionId, e)
      )

    def buildExecution(id: ExecutionId, executionDetails: ExecutionDetails, cancel: ExecutionId => Unit, remove: ExecutionId => Unit)(using panels: Panels) =
      elementTable()
        .addRow(executionRow(id, executionDetails, cancel, remove)).expandTo(expander(id, executionDetails), showExpander.signal.map(_.isDefined))
        .unshowSelection
        .render.render.amend(idAttr := "exec")

    def envErrorLevelToColor(level: ErrorStateLevel) =
      level match
        case ErrorStateLevel.Error => "#c8102e"
        case _ => "#555"

    def environmentControls(id: EnvironmentId, clear: EnvironmentId => Unit) = div(cls := "execButtons",
      child <-- showEvironmentControls.signal.map: c =>
        if c
        then
          div(display.flex, flexDirection.column, alignItems.center,
            button("Clear", onClick --> { _ => clear(id) }, btn_danger, cls := "controlButton")
          )
        else div()
    )


    def jobRow(executionId: ExecutionId, e: EnvironmentState) =
      val openEnvironmentErrors: Var[Boolean] = Var(false)

      def cleanEnvironmentErrors(executionId: ExecutionId, id: EnvironmentId) =
        api.clearEnvironmentError(executionId, id).andThen: _ =>
          showExpander.set(None)
          showExpander.set(Some(Expand.Computing))

      div(columnFlex,
        div(rowFlex, justifyContent.center,
          contextBlock("Resource", e.taskName, true, blockWidth = 180),
          contextBlock("Execution time", CoreUtils.approximatedYearMonthDay(e.executionActivity.executionTime), true, blockWidth = 220),
          contextBlock("Uploads", displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize, e.networkActivity.uploadingFiles), true, blockWidth = 200),
          contextBlock("Downloads", displaySize(e.networkActivity.downloadedSize, e.networkActivity.readableDownloadedSize, e.networkActivity.downloadingFiles), true),
          contextBlock("Submitted", e.submitted.toString, true),
          contextBlock("Running", e.running.toString, true),
          contextBlock("Finished", e.done.toString, true),
          contextBlock("Failed", e.failed.toString, true),
          contextBlock("Errors", e.numberOfErrors.toString, true, link = true).amend(
            onClick --> openEnvironmentErrors.update(!_), cursor.pointer),
          div(cls := "bi-three-dots-vertical execControls", onClick --> showEvironmentControls.update(!_)),
          environmentControls(e.envId, id => cleanEnvironmentErrors(executionId, id)),
        ),
        child <-- openEnvironmentErrors.signal.map: opened =>
          if opened
          then
            div(width := "100%", height := "400px",
              overflow.scroll,
              children <-- Signal.fromFuture(api.listEnvironmentError(executionId, e.envId, 200)).map:
                case Some(ee) =>
                  val errors = ee.filter(_.level == ErrorStateLevel.Error).sortBy(_.date).reverse ++ ee.filter(_.level != ErrorStateLevel.Error).sortBy(_.date).reverse
                  errors.zipWithIndex.map: (e, i) =>
                    div(flexRow,
                      cls := "docEntry", width := "100%",
                      backgroundColor := { if i % 2 == 0 then "#bdadc4" else "#f4f4f4" },
                      div(CoreUtils.longTimeToString(e.date), minWidth := "100"),
                      a(e.errorMessage, float.left, color := "#222", cursor.pointer, flexGrow := "4"),
                      div(btn_danger, e.level.name)
                    ).expandOnclick(
                      div(height := "200", overflow.scroll, ClientUtil.errorTextArea(stackTrace(e.stack)))
                    )
                case None => Seq()
            )
          else div()
      )

    def expander(id: ExecutionId, details: ExecutionDetails) =
      div(height := "500", rowFlex, justifyContent.center,
        child <-- showExpander.signal.map:
          case Some(Expand.Script) => div(execTextArea(details.script))
          case Some(Expand.Console) =>
            val size = Var(1000)
            //val ansi: Var[Boolean] = Var(true)

            div(
              child <--
                size.signal.flatMap: sizeValue =>
                  Signal.fromFuture(api.executionOutput(id, sizeValue)).map:
                    case Some(output) =>
                      val Convert = scalajs.js.Dynamic.global.require("./node_modules/ansi-to-html/lib/ansi_to_html.js")
                      val convert = scalajs.js.Dynamic.newInstance(Convert)()
                      convert.options.newline = true

                      def baseOutputDiv = div(fontFamily := "monospace", fontSize := "medium", cls := "execTextArea", overflow := "scroll", marginTop := "0", marginBottom := "10")

                      def ansiOutputDiv =
                        val d = baseOutputDiv
                        d.ref.innerHTML = convert.toHtml(output.output.replace(" ", "&nbsp;")).asInstanceOf[String]
                        d

                      def rawOutputDiv =
                        val d = baseOutputDiv
                        d.ref.innerHTML = output.output.replace(" ", "&nbsp;").replace("\n", "<br/>")
                        d

                      val ansiSwitch = Component.Switch("ansi", true)

                      val outputDiv =
                        ansiSwitch.checked.signal.map:
                          case true => ansiOutputDiv
                          case false => rawOutputDiv

                      def more =
                        if output.listed < output.total
                        then
                          div(cursor.pointer, display.flex, justifyContent.flexEnd, alignItems.center,
                            i(s"${output.listed}/${output.total}"),
                            i(cls := "bi bi-plus plusButton"),
                            onClick --> { _ => size.update(_ * 2) }
                          )
                        else div()

                      div(
                        display.flex, flexDirection.row, marginTop := "50px",
                        height := "500",
                        child <-- outputDiv,
                        outputDiv.toObservable --> Observer[ReactiveElement[HTMLDivElement]](e => e.ref.scrollTop = e.ref.scrollHeight),
                        div(
                          columnFlex,
                          marginTop := "20px",
                          ansiSwitch.element.amend(justifyContent.flexEnd),
                          more
                        )
                      )
                      //outputDiv ++ more ++ Seq(bottom)
                    case None => i(cls := "bi bi-hourglass-split", textAlign := "center")
            )
          case Some(Expand.ErrorLog) => div(execTextArea(details.error.map(ErrorData.stackTrace).getOrElse("")))
          case Some(Expand.Computing) => jobs(id, details.envStates)
          case None => div()
      )

    val forceUpdate = Var(0)
    def triggerStateUpdate = forceUpdate.update(_ + 1)

    val initialUpdate = EventStream.delay(500)
    val periodicUpdate = EventStream.periodic(10000).drop(1, resetOnStop = true).filter(_ => !queryingState && !showExpander.now().isDefined)

    div(
      columnFlex, width := "100%", marginTop := "20",
      div(cls := "close-button bi-x", backgroundColor := "#bdadc4", borderRadius := "20px", onClick --> panels.closeExpandable),
      (initialUpdate.startWithNone combineWith periodicUpdate.startWithNone combineWith forceUpdate.signal).toObservable -->
        Observer[(Option[Unit], Option[Int], Int)]: (i, p, u) =>
          if !queryingState && (u >= 1 || i.isDefined)
          then
            queryState.foreach: allDetails =>
              val (d, toClean) = filterExecutions(allDetails)
              toClean.foreach(api.removeExecution)
              details.set(d)
      ,
      children <--
        (details.signal combineWith currentOpenSimulation.signal).map: (details, id) =>
          Seq(
            div(rowFlex, justifyContent.center,
              details.toSeq.sortBy(_._2.startDate).map { (id, detailValue) => simulationBlock(id, detailValue) }
            ),
            div(
              id.map: idValue =>
                details.get(idValue) match
                  case Some(st) =>
                    def cancel(id: ExecutionId) = api.cancelExecution(id).andThen { case Success(_) => triggerStateUpdate }
                    def remove(id: ExecutionId) = api.removeExecution(id).andThen { case Success(_) => triggerStateUpdate }

                    div(buildExecution(idValue, st, cancel, remove))
                  case None => div()
            )
          )
      ,
      div(display.flex, flexDirection.row, alignItems.center, cls := "execGlobalControls", height := "100",
        child <--
          showGlobalControls.signal.map:
            case true =>
              div(display.flex, flexDirection.column, marginRight := "10",
                button(btn_danger, "Clean inactive", //cls := "removeInactive",
                  width := "180",
                  marginBottom := "5",
                  onClick --> { _ =>
                    Future.sequence:
                      details.now().filter((_, d) => ExecutionDetails.State.isFinished(d.state)).map((e, _) => api.removeExecution(e))
                    .andThen: _ =>
                      triggerStateUpdate
                  }
                ),
                autoRemoveFailed.element.amend(width := "180", marginTop := "5")
              )
            case false => emptyNode,
        div(cls := "bi-three-dots-vertical execControls", color := "#373f46", backgroundColor := "white",
          onClick --> showGlobalControls.update(!_))
      ),
      showExpander.toObservable.distinct --> Observer { e => if e == None then triggerStateUpdate },
      currentOpenSimulation.toObservable -->
        Observer: _ =>
          showControls.set(false)
          showExpander.set(None)
    )