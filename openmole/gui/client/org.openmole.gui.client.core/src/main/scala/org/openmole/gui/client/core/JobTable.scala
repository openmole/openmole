package org.openmole.gui.client.core

import org.openmole.gui.client.core.ExecutionPanel.{ CapsuleView, EnvironmentView, JobView, SubScript }
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.tool.client.omsheet
import scaladget.bootstrapnative.bsn
import bsn._
import org.openmole.gui.ext.api.Api
import scaladget.tools
import scaladget.tools._
import scalatags.JsDom.all._
import rx._
import scaladget.bootstrapnative.Table.{ BSTableStyle, FixedCell, ReactiveRow, SubRow, VarCell }
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._

import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetTimeoutHandle

object JobTable {
  def apply(executionId: ExecutionId) = new JobTable(executionId)

  private def displaySize(size: Long, readable: String) =
    if (size == 0L) ""
    else s"($readable)"

  //val expandedErrors: Var[Map[EnvironmentId, Boolean]] = Var(Map())
}

import JobTable._

class JobTable(executionId: ExecutionId) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val jobViews: Var[JobView] = Var(CapsuleView)

  val capsuleActivated = jobViews.now == CapsuleView

  val jobViewButton = bsn.radios(omsheet.centerElement +++ Seq(marginTop := 15, marginBottom := 20))(
    bsn.selectableButton("Capsules", capsuleActivated, onclick = () ⇒ switchView),
    bsn.selectableButton("Environments", !capsuleActivated, onclick = () ⇒ switchView)
  )

  //val envErrors: Var[Map[EnvironmentId, EnvironmentErrorData]] = Var(Map())

  val envError: Var[Map[EnvironmentId, EnvironmentErrorData]] = Var(Map())
  //val environmentStates: Var[Seq[Var[EnvironmentState]]] = Var(Seq())

  val errOpen = Var(false)

  def updateEnvErrors(environmentId: EnvironmentId) =
    // if (!errOpen.now) {
    post()[Api].runningErrorEnvironmentData(environmentId, panels.executionPanel.envErrorHistory.value.toInt).call().foreach {
      err ⇒
        println("ERR")
        envError() = envError.now + (environmentId → err)
    }
  //}

  def clearEnvErrors(environmentId: EnvironmentId) =
    post()[Api].clearEnvironmentErrors(environmentId).call().foreach {
      _ ⇒
        envError() = envError.now - environmentId
    }

  //  panels.executionPanel.envError.trigger {
  //    println("TRRRRIGER " + errOpen.now)
  //    if (!errOpen.now) {
  //      println("--------------------- Update errors")
  //      envErrors() = panels.executionPanel.envError.now.map { case (k, v) ⇒ (k, v._1) }
  //    }
  //  }

  def switchView = {
    jobViews() = jobViews.now match {
      case EnvironmentView ⇒ CapsuleView
      case _               ⇒ EnvironmentView
    }
  }

  val executionInfo: Var[Option[ExecutionInfo]] = Var(None)

  def delay: SetTimeoutHandle = {
    timers.setTimeout(8000) {
      println("Update envs")
      panels.executionPanel.executionInfo.now.filter(_._1 == executionId).map {
        _._2
      }.headOption.foreach { e ⇒
        executionInfo() = Some(e)
      }
      delay
    }
  }

  delay

  //  def execInfos = panels.executionPanel.executionInfo.map {
  //    _.executionInfo.filter(_._1 == executionId).headOption
  //  }

  val environmentStates = executionInfo.map {
    _.map {
      _.environmentStates
    }.getOrElse(Seq())
  }

  //  val environmentStates = panels.executionPanel.executionInfo.map { ei =>
  //    ei.get(executionId).map {
  //      _.environmentStates.map {
  //        _
  //        .
  //      }
  //    }
  //  }

  def capsuleTable(info: ExecutionInfo) = {
    scaladget.bootstrapnative.DataTable(
      Some(scaladget.bootstrapnative.Table.Header(Seq("Name", "Running", "Completed"))),
      info.capsules.map { c ⇒ scaladget.bootstrapnative.DataTable.DataRow(Seq(c._1.toString, c._2.running.toString, c._2.completed.toString)) },
      scaladget.bootstrapnative.Table.BSTableStyle(bsn.bordered_table, tools.emptyMod), true).render(width := "100=").render
  }

  def error(errors: Map[EnvironmentId, EnvironmentErrorData], environmentId: EnvironmentId) = errors.getOrElse(environmentId, EnvironmentErrorData.empty)

  val environmentTable /*(envS: Seq[EnvironmentState], errors: Map[EnvironmentId, EnvironmentErrorData])*/ = {
    scaladget.bootstrapnative.Table(
      //        ReactiveRow(
      //          execID.id,
      //          Seq(
      //            VarCell(tags.span(subLink(SubScript, execID, staticInfo.now(execID).path.name).tooltip("Original script")), 0),
      //
      for {
        ee ← environmentStates
      } yield {
        ee.map { e ⇒
          ReactiveRow(
            e.envId.id,
            Seq(
              FixedCell(span(e.taskName), 0),
              VarCell(span(CoreUtils.approximatedYearMonthDay(e.executionActivity.executionTime)), 1),
              VarCell(span(displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize)), 2),
              VarCell(span(displaySize(e.networkActivity.downloadedSize, e.networkActivity.readableDownloadedSize)), 3),
              VarCell(span(e.submitted.toString), 4),
              VarCell(span(e.running.toString), 5),
              VarCell(span(e.done.toString), 6),
              VarCell(span(e.failed.toString), 7),
              VarCell(if (e.numberOfErrors == 0) span() else
                button("Errors", onclick := { () ⇒
                  errOpen() = !errOpen.now
                  //  val previousState = expandedErrors.now.get(e.envId).getOrElse(false)
                  //   expandedErrors() = expandedErrors.now.updated(e.envId, !previousState)
                  updateEnvErrors(e.envId)
                  //   panels.executionPanel.toggleEnvironmentErrorPanel(e.envId)
                }, bsn.btn_danger)(badge(e.numberOfErrors.toString)), 8),
              // span(bsn.buttonGroup(omsheet.columnLayout +++ (width := 80))(
              // bsn.buttonIcon(glyphicon = bsn.glyph_refresh, todo = { () ⇒ panels.executionPanel.updateEnvErrors(e.envId) }).tooltip("Refresh environment errors"),
              FixedCell(bsn.buttonIcon(buttonStyle = bsn.btn_default, glyphicon = bsn.glyph_repeat, todo = () ⇒ clearEnvErrors(e.envId)), 9) /*).tooltip("Reset environment errors")*/
            )
          )
        }
      },
      subRow = Some((i: scaladget.tools.ID) ⇒ SubRow(
        envError.map {
          _.get(EnvironmentId(i))
        }.map { env ⇒
          env match {
            case Some(e: EnvironmentErrorData) ⇒
              val panel = new EnvironmentErrorPanel(e)
              //              envError.map { ee ⇒
              //                panel.setErrors(error(ee, EnvironmentId(i)))
              //                panel.view.render
              //              }
              div(panel.view.render)
            case None ⇒ div("No env")
          }
        }, errOpen)),
      bsTableStyle = BSTableStyle(tableStyle = `class` := "table executionTable")
    ).addHeaders("Name", "Elapsed time", "Uploads", "Downloads", "Submitted", "Running", "Finished", "Failed", "Errors", "Actions")
      .render(minWidth := 1000)

    //              div(panels.executionPanel.staticPanel(e.envId, panels.executionPanel.envErrorPanels,
    //                () ⇒ new EnvironmentErrorPanel,
    //                (ep: EnvironmentErrorPanel) ⇒
    //                  ep.setErrors(error(errors, e.envId))).view)
    //  ,
    // errOpen
    //                {
    //                  println("EXPERROR " + expandedErrors)
    //                  expandedErrors.map {
    //                    _.getOrElse(e.envId, false)
    //                  }
    //                }

    //  scaladget.bootstrapnative.Table.BSTableStyle(bsn.bordered_table, tools.emptyMod)

  }

  val render =
    Rx {
      if (environmentStates().isEmpty) Waiter.waiter
      else {
        div(backgroundColor := "#fff", border := "1px solid #ececec")(
          jobViewButton.render,
          if (jobViews() == CapsuleView) {
            executionInfo() match {
              case Some(info) ⇒ capsuleTable(info)
              case _          ⇒ println("wait capsules")
            }

          }
          else {
            environmentStates() match {
              case states: Seq[EnvironmentState] ⇒ environmentTable //(states, envErrors())
              case _                             ⇒ div("wait ENOVSS")
            }
          }
        )
      }
    }

}