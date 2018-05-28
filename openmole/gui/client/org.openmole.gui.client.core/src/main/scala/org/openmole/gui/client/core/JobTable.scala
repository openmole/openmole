package org.openmole.gui.client.core

import org.openmole.gui.client.core.ExecutionPanel.{ CapsuleView, EnvironmentView }
import org.openmole.gui.ext.data.{ EnvironmentErrorData, EnvironmentId, ExecutionId, ExecutionInfo }
import org.openmole.gui.ext.tool.client.omsheet
import scaladget.bootstrapnative.bsn
import bsn._
import scaladget.tools
import scaladget.tools._
import scalatags.JsDom.all._
import rx._
import scaladget.bootstrapnative.Table.SubRow

object JobTable {
  def apply(executionId: ExecutionId, executionInfo: ExecutionInfo) = new JobTable(executionId, executionInfo)

  private def displaySize(size: Long, readable: String) =
    if (size == 0L) ""
    else s"($readable)"

}

import JobTable._

class JobTable(executionId: ExecutionId, executionInfo: ExecutionInfo) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val capsuleActivated = panels.executionPanel.jobViews.now(executionId) == CapsuleView
  val jobViewButton = bsn.radios(omsheet.centerElement +++ Seq(marginTop := 15, marginBottom := 20))(
    bsn.selectableButton("Capsules", capsuleActivated, onclick = () ⇒ {
      panels.executionPanel.jobViews() = panels.executionPanel.jobViews.now ++ Map(executionId -> CapsuleView)
    }),
    bsn.selectableButton("Environments", !capsuleActivated,
      onclick = () ⇒ panels.executionPanel.jobViews() = panels.executionPanel.jobViews.now ++ Map(executionId -> EnvironmentView))
  )

  val capsuleTable = scaladget.bootstrapnative.DataTable(
    Some(scaladget.bootstrapnative.Table.Header(Seq("Name", "Running", "Completed"))),
    executionInfo.capsules.map { c ⇒ scaladget.bootstrapnative.DataTable.DataRow(Seq(c._1.toString, c._2.running.toString, c._2.completed.toString)) },
    scaladget.bootstrapnative.Table.BSTableStyle(bsn.bordered_table, tools.emptyMod), true)

  val envErrorVisible: Var[Seq[EnvironmentId]] = Var(Seq())

  def isVisible(id: EnvironmentId): Var[Boolean] = Var(envErrorVisible.map { s ⇒ s.contains(id) }.now)

  def toggleEnvironmentErrorPanel(envID: EnvironmentId) = { () ⇒
    if (envErrorVisible.now.contains(envID)) envErrorVisible() = envErrorVisible.now.filterNot {
      _ == envID
    }
    else envErrorVisible() = envErrorVisible.now :+ envID
  }

  def error(errors: Map[EnvironmentId, EnvironmentErrorData], environmentId: EnvironmentId) = errors.getOrElse(environmentId, EnvironmentErrorData.empty)

  val environmentTable = {
    val errors = panels.executionPanel.envError.now

    println("errors " + errors)

    scaladget.bootstrapnative.Table(
      Some(scaladget.bootstrapnative.Table.Header(Seq("Name", "Elapsed time", "Uploads", "Downloads", "Submitted", "Running", "Finished", "Failed", "Errors", "Actions"))),
      executionInfo.environmentStates.map { e ⇒

        println("Env id " + e.envId)
        println("errors ID" + error(errors, e.envId))
        println("empty ?" + error(errors, e.envId) == EnvironmentErrorData.empty)
        scaladget.bootstrapnative.Table.Row(
          Seq(
            span(e.taskName),
            span(CoreUtils.approximatedYearMonthDay(e.executionActivity.executionTime)),
            span(displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize)),
            span(displaySize(e.networkActivity.downloadedSize, e.networkActivity.readableDownloadedSize)),
            span(e.submitted.toString),
            span(e.running.toString),
            span(e.done.toString),
            span(e.failed.toString),
            if (error(errors, e.envId) == EnvironmentErrorData.empty) span() else button(bsn.btn_danger, "Errors", onclick := toggleEnvironmentErrorPanel(e.envId)),
            span(bsn.buttonGroup(omsheet.columnLayout +++ (width := 80))(
              bsn.buttonIcon(glyphicon = bsn.glyph_refresh, todo = { () ⇒ panels.executionPanel.updateEnvErrors(e.envId) }).tooltip("Refresh environment errors"),
              bsn.buttonIcon(buttonStyle = bsn.btn_default, glyphicon = bsn.glyph_repeat, todo = () ⇒ panels.executionPanel.clearEnvErrors(e.envId))).tooltip("Reset environment errors"))),
          subRow = Some(SubRow(
            div(panels.executionPanel.staticPanel(e.envId, panels.executionPanel.envErrorPanels,
              () ⇒ new EnvironmentErrorPanel,
              (ep: EnvironmentErrorPanel) ⇒
                ep.setErrors(error(errors, e.envId))).view), isVisible(e.envId))))
      },
      scaladget.bootstrapnative.Table.BSTableStyle(bsn.bordered_table, tools.emptyMod)).render(width := "100%")
  }

  def render = div(
    jobViewButton.render,
    rxIf(panels.executionPanel.jobViews.map {
      _(executionId) == CapsuleView
    }, capsuleTable.render(width := "100="), environmentTable
    )
  )

}