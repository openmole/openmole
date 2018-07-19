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
}

import JobTable._

class JobTable(executionId: ExecutionId) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val jobViews: Var[JobView] = Var(CapsuleView)

  val capsuleActivated = jobViews.now == CapsuleView

  val jobViewButton = bsn.radios(omsheet.centerElement +++ Seq(marginTop := 40, marginBottom := 40))(
    bsn.selectableButton("Capsules", capsuleActivated, onclick = () ⇒ toCapsuleView),
    bsn.selectableButton("Environments", !capsuleActivated, onclick = () ⇒ toEnvironmentView)
  )

  val envError: Var[Map[EnvironmentId, EnvironmentErrorData]] = Var(Map())
  val errOpen: Var[Map[EnvironmentId, Boolean]] = Var(Map())
  val autoUpdateErrors = Var(false)

  def updateEnvErrors: Unit = envError.now.keys.foreach {
    updateEnvErrors(_)
  }

  def updateEnvErrors(environmentId: EnvironmentId) =
    post()[Api].runningErrorEnvironmentData(environmentId, panels.executionPanel.envErrorHistory.value.toInt).call().foreach {
      err ⇒
        envError() = envError.now + (environmentId → err)
    }

  def clearEnvErrors(environmentId: EnvironmentId) =
    post()[Api].clearEnvironmentErrors(environmentId).call().foreach {
      _ ⇒
        envError() = envError.now - environmentId
    }

  def toCapsuleView = jobViews() = CapsuleView

  def toEnvironmentView = jobViews() = EnvironmentView

  val executionInfo: Var[Option[ExecutionInfo]] = Var(None)

  def delay: SetTimeoutHandle = {
    timers.setTimeout(8000) {
      panels.executionPanel.executionInfo.now.filter(_._1 == executionId).map {
        _._2
      }.headOption.foreach { e ⇒
        executionInfo() = Some(e)
      }
      delay
    }
  }

  delay

  val environmentStates = executionInfo.map {
    _.map {
      _.environmentStates
    }.getOrElse(Seq())
  }

  def allErrors(states: Seq[EnvironmentState]) = states.map { es ⇒ es.envId -> es.numberOfErrors }.toMap

  val oldNumberErrors = Var(allErrors(environmentStates.now))

  def capsuleTable(info: ExecutionInfo) = {
    scaladget.bootstrapnative.DataTable(
      Some(scaladget.bootstrapnative.Table.Header(Seq("Name", "Running", "Completed"))),
      info.capsules.map {
        c ⇒ scaladget.bootstrapnative.DataTable.DataRow(Seq(c._1.toString, c._2.running.toString, c._2.completed.toString))
      },
      scaladget.bootstrapnative.Table.BSTableStyle(bsn.bordered_table, tools.emptyMod), true).render(width := "100%").render
  }

  def error(errors: Map[EnvironmentId, EnvironmentErrorData], environmentId: EnvironmentId) = errors.getOrElse(environmentId, EnvironmentErrorData.empty)

  val environmentTable = {
    scaladget.bootstrapnative.Table(
      for {
        ee ← environmentStates
      } yield {
        ee.map { e ⇒
          if (autoUpdateErrors.now) updateEnvErrors(e.envId)
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
                  errOpen.update(errOpen.now.updated(e.envId, !errOpen.now.getOrElse(e.envId, false)))
                  updateEnvErrors(e.envId)
                }, bsn.btn_danger)(badge(e.numberOfErrors.toString)), 8)
            )
          )
        }
      },
      subRow = Some((i: scaladget.tools.ID) ⇒ SubRow(
        envError.map {
          _.get(EnvironmentId(i))
        }.map {
          env ⇒
            env match {
              case Some(e: EnvironmentErrorData) ⇒
                val panel = new EnvironmentErrorPanel(e, EnvironmentId(i), this)
                div(panel.view.render)
              case None ⇒ div("")
            }
        }, errOpen.map { _.getOrElse(EnvironmentId(i), false) })),
      bsTableStyle = BSTableStyle(tableStyle = `class` := "table executionTable")
    ).addHeaders("Name", "Elapsed time", "Uploads", "Downloads", "Submitted", "Running", "Finished", "Failed", "Errors")
      .render(minWidth := 1000)
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
              case _          ⇒ Waiter.waiter
            }
          }
          else {
            environmentStates() match {
              case states: Seq[EnvironmentState] ⇒ environmentTable
              case _                             ⇒ Waiter.waiter
            }
          }
        )
      }
    }

}