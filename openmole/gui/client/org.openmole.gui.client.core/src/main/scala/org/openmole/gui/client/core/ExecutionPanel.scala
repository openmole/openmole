package org.openmole.gui.client.core

import java.util.concurrent.atomic.AtomicBoolean
import scala.util.{Failure, Success}
import org.openmole.gui.ext.client.*

import scala.scalajs.js.timers.*
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.data.ErrorData as ExecError
import org.openmole.gui.ext.data.*
import org.openmole.gui.client.core.alert.{BannerAlert, BannerLevel}
import org.openmole.gui.client.core.files.{OMSContent, TabContent, TreeNodeTabs}
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.ext.client.Utils
import org.openmole.gui.ext.data.ExecutionInfo.Failed
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.Panels.ExpandablePanel

import concurrent.duration.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*

object ExecutionPanel:
  case class ExInfo(id: ExecutionId, info: Var[ExecutionInfo])

  case class ExecutionDetails(status: String,
                              duration: Long,
                              ratio: String,
                              running: Long,
                              error: Option[ExecError] = None,
                              envStates: Seq[EnvironmentState] = Seq(),
                              outputs: String = "")

  type Statics = Map[ExecutionId, StaticExecutionInfo]
  type Execs = Map[ExecutionId, ExecutionDetails]

  def open(using fetch: Fetch, panels: Panels) =
    panels.executionPanel.setTimerOn
    panels.executionPanel.updateStaticInfos
    panels.executionPanel.updateExecutionInfo
    Panels.expandTo(panels.executionPanel.render, 4)


class ExecutionPanel:
  import ExecutionPanel.*

  val staticInfos: Var[ExecutionPanel.Statics] = Var(Map())
  val executionDetails: Var[ExecutionPanel.Execs] = Var(Map())
  val outputInfos: Var[Seq[OutputStreamData]] = Var(Seq())
  val timerOn = Var(false)
  val currentOpenSimulation: Var[Option[ExecutionId]] = Var(None)

  val updating = new AtomicBoolean(false)

  def setTimerOn = {
    updating.set(false)
    timerOn.set(true)
  }

  def setTimerOff = timerOn.set(false)

  def toExecDetails(exec: ExecutionInfo): ExecutionDetails = {
    exec match {
      case f: ExecutionInfo.Failed ⇒ ExecutionDetails(if (!f.clean) "cleaning" else exec.state, exec.duration, "0", 0, Some(f.error), f.environmentStates)
      case f: ExecutionInfo.Finished ⇒ ExecutionDetails(if (!f.clean) "cleaning" else exec.state, exec.duration, ratio(f.completed, f.running, f.ready), f.running, envStates = f.environmentStates)
      case r: ExecutionInfo.Running ⇒ ExecutionDetails(exec.state, exec.duration, ratio(r.completed, r.running, r.ready), r.running, envStates = r.environmentStates)
      case c: ExecutionInfo.Canceled ⇒ ExecutionDetails(if (!c.clean) "cleaning" else exec.state, exec.duration, "0", 0, envStates = c.environmentStates)
      case r: (ExecutionInfo.Compiling | ExecutionInfo.Preparing) ⇒ ExecutionDetails(exec.state, exec.duration, "0", 0, envStates = r.environmentStates)
    }
  }

  def updateScriptError(path: SafePath, details: ExecutionDetails)(using panels: Panels) = OMSContent.setError(path, details.error)

  def updateExecutionInfo(using fetch: Fetch): Unit = {

    def delay =
      updating.set(false)
      setTimeout(5000) {
        //println("UPDATE")
        updateExecutionInfo
        if (staticInfos.now().size != executionDetails.now().size) updateStaticInfos
      }

    if (updating.compareAndSet(false, true)) {
      fetch.future(_.allStates(200).future).andThen {
        case Success((execInfos, runningOutputData)) ⇒
          executionDetails.set(execInfos.map { case (k, v) =>
            k -> toExecDetails(v)
          }.toMap)
          outputInfos.set(runningOutputData)
          //println("output infos now " + outputInfos.now().head)
          if (timerOn.now()) delay
        case Failure(_) ⇒ delay
      }
    }
  }

  def updateStaticInfos(using fetch: Fetch) = fetch.future(_.staticInfos(()).future).foreach { s ⇒
    staticInfos.set(s.toMap)
    //println("Statis infos now " + staticInfos.now().head._1)
    setTimeout(0) { updateExecutionInfo }
  }


  val rowFlex = Seq(display.flex, flexDirection.row, alignItems.center)
  val columnFlex = Seq(display.flex, flexDirection.column, justifyContent.flexStart)

  sealed trait Expand

  object Console extends Expand

  object Script extends Expand

  object ErrorLog extends Expand


  val showDurationOnCores = Var(false)
  val showExpander: Var[Option[Expand]] = Var(None)
  val showControls = Var(false)

  def contextBlock(info: String, content: String) = {
    div(columnFlex, div(cls := "contextBlock", div(info, cls := "info"), div(content, cls := "infoContent")))
  }

  def statusBlock(info: String, content: String) = {
    statusBlockFromDiv(info, div(content, cls := "infoContent"), "statusBlock")
  }

  def statusBlockFromDiv(info: String, contentDiv: Div, blockCls: String) = {
    div(columnFlex, div(cls := blockCls, div(info, cls := "info"), contentDiv))
  }

  def scriptBlock(scriptName: String) =
    div(columnFlex, div(cls := "contextBlock",
      cls <-- showExpander.signal.map { exp =>
        if (exp == Some(Script)) "statusOpen"
        else ""
      },
      div("Script", cls := "info"),
      div(scriptName, cls := "infoContentLink")),
      onClick --> { _ =>
        showExpander.update(exp =>
          if (exp == Some(Script)) None
          else Some(Script)
        )
      },
      cursor.pointer
    )

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
      onClick --> { _ => showDurationOnCores.update(!_) },
      cursor.pointer
    )

  def consoleBlock =
    div(columnFlex, div(cls := "statusBlock",
      cls.toggle("", "statusOpen") <-- showExpander.signal.map {
        _ == Some(Console)
      },
      div("Standard output", cls := "info"),
      div(child <-- showExpander.signal.map { c => if (c == Some(Console)) "Hide" else "Show" }, cls := "infoContentLink")),
      onClick --> { _ =>
        showExpander.update(exp =>
          if (exp == Some(Console)) None
          else Some(Console)
        )
      },
      cursor.pointer
    )

  def simulationStatusBlock(status: String) =
    div(columnFlex, div(cls := "statusBlockNoColor",
      cls.toggle("", "statusOpen") <-- showExpander.signal.map {
        _ == Some(ErrorLog)
      },
      div("Status", cls := "info"),
      div(status.capitalize, cls := {
        if (status == "failed") "infoContentLink"
        else "infoContent"
      }),
      onClick --> { _ =>
        showExpander.update(exp =>
          if (exp == Some(ErrorLog)) None
          else Some(ErrorLog)
        )
      },
      cursor.pointer
    )
    )


  val controls = div(cls := "execButtons",
    child <-- showControls.signal.map { c =>
      if (c)
        div(
          button("Stop", onClick --> { _ => println("Delete") }, btn_danger, cls := "controlButton", marginLeft := "20"),
          button("Clean", onClick --> { _ => println("Clean") }, btn_secondary, cls := "controlButton"),
        )
      else div()
    }
  )

  def ratio(completed: Long, running: Long, ready: Long) = s"${
    completed
  } / ${
    completed + running + ready
  }"

  def executionRow(staticInfo: StaticExecutionInfo, details: ExecutionDetails) = {
    div(rowFlex, justifyContent.center,
      scriptBlock(staticInfo.path.name),
      contextBlock("Start time", Utils.longToDate(staticInfo.startDate)),
      contextBlock("Method", "???"),
      durationBlock(details.duration, 0L),
      statusBlock("Running", details.running.toString),
      statusBlock("Completed", details.ratio),
      simulationStatusBlock(details.status).amend(backgroundColor := statusColor(details.status)),
      consoleBlock,
      div(cls := "bi-three-dots-vertical execControls", onClick --> { _ => showControls.update(!_) }),
      controls
    )
  }

  def execTextArea(content: String): HtmlElement = textArea(content, idAttr := "execTextArea")


  val expander = div(height := "500",
    child <-- showExpander.signal.map {
      _ match {
        case Some(Script) => div(
          child <-- staticInfos.signal.combineWith(currentOpenSimulation.signal).map { case (statics, id) =>
            execTextArea(id.flatMap { i =>
              statics.get(i).map(_.script)
            }.getOrElse("")
            )
          }
        )
        case Some(Console) =>
          div(child <-- outputInfos.signal.combineWith(currentOpenSimulation).map { case (oi, execID) ⇒
            val cont = oi.find(o => Some(o.id) == execID).map(_.output).getOrElse("")
            println("Cont: " + cont)
            execTextArea(cont).amend(cls := "console")
          }
          )
        case Some(ErrorLog) =>
          div(child <-- executionDetails.signal.combineWith(currentOpenSimulation.signal).map { case (execD, id) =>
            execTextArea(
              id.flatMap { i =>
                execD.get(i).flatMap(_.error.map(ExecError.stackTrace))
              }.getOrElse("")
            )
          }
          )
        case None => div()
      }
    }
  )

  def buildExecution(static: StaticExecutionInfo, executionDetails: ExecutionDetails)(using panels: Panels) = {
    OMSContent.setError(static.path, executionDetails.error)
    elementTable()
      .addRow(executionRow(static, executionDetails)).expandTo(expander, showExpander.signal.map(_.isDefined))
      .unshowSelection
      .render.render.amend(idAttr := "exec")
  }

  def statusColor(status: String) = status match {
    case "completed" => "#00810a"
    case "failed" => "#c8102e"
    case "canceled" => "#d14905"
    case "preparing" | "compiling" => "#f1c306"
    case "running" => "#a5be21"
  }

  def simulationBlock(executionId: ExecutionId, staticExecutionInfo: StaticExecutionInfo, executionInfo: ExecutionDetails) =
    div(rowFlex, justifyContent.center, alignItems.center,
      cls := "simulationInfo",
      cls.toggle("statusOpenSim") <-- currentOpenSimulation.signal.map { os => os == Some(executionId) },
      div("", cls := "simulationID", backgroundColor := statusColor(executionInfo.status)),
      div(staticExecutionInfo.path.nameWithNoExtension),
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

  def render(using panels: Panels) = div(columnFlex, width := "100%", marginTop := "20",
    children <-- staticInfos.signal.combineWith(executionDetails.signal).combineWith(currentOpenSimulation.signal).map { case (statics, execs, id) =>
      println("00 " + statics.keys + " // " + execs.keys + " // " + id)
      Seq(
        div(rowFlex, justifyContent.center,
          statics.toSeq.map { case (id, st) =>
            println("001 " + id)
            simulationBlock(id, st, execs(id))
          }
        ),
        div(
          id.map { i =>
            val static = statics.get(i)
            static match {
              case Some(st) =>
                println("003 " + i + " / " + statics(i))
                div(buildExecution(st, execs(i)))
              case None =>
                println("NONE")
                div()
            }
          }
        )
      )
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



