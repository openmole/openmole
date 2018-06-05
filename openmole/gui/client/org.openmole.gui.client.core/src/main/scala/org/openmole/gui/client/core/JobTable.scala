package org.openmole.gui.client.core

import org.openmole.gui.client.core.ExecutionPanel.{ CapsuleView, EnvironmentView }
import org.openmole.gui.ext.data.{ ExecutionId, ExecutionInfo }
import org.openmole.gui.ext.tool.client.omsheet
import scaladget.bootstrapnative.bsn
import scaladget.tools
import scaladget.tools._
import scalatags.JsDom.all._

object JobTable {
  def apply(executionId: ExecutionId, executionInfo: ExecutionInfo) = new JobTable(executionId, executionInfo)

  private def displaySize(size: Long, readable: String) =
    if (size == 0L) ""
    else s"($readable)"

}

import JobTable._

class JobTable(executionId: ExecutionId, executionInfo: ExecutionInfo) {

  val capsuleActivated = panels.executionPanel.jobViews.now(executionId) == CapsuleView
  val jobViewButton = bsn.radios(omsheet.centerElement +++ Seq(marginTop := 15, marginBottom := 20))(
    bsn.selectableButton("Capsules", capsuleActivated, onclick = () ⇒ {
      panels.executionPanel.jobViews() = panels.executionPanel.jobViews.now ++ Map(executionId -> CapsuleView)
    }),
    bsn.selectableButton("Environments", !capsuleActivated,
      onclick = () ⇒ panels.executionPanel.jobViews() = panels.executionPanel.jobViews.now ++ Map(executionId -> EnvironmentView))
  )

  val capsuleTable = scaladget.bootstrapnative.Table(
    Some(scaladget.bootstrapnative.Row(Seq("Name", "Running", "Completed"))),
    executionInfo.capsules.map { c ⇒ scaladget.bootstrapnative.Row(Seq(c._1.toString, c._2.running.toString, c._2.completed.toString)) },
    scaladget.bootstrapnative.BSTableStyle(bsn.bordered_table, tools.emptyMod), true)

  val environmentTable = scaladget.bootstrapnative.Table(
    Some(scaladget.bootstrapnative.Row(Seq("Name", "Elapsed time", "Uploads", "Downloads", "Submitted", "Runnirg", "Finished", "Failed"))),
    executionInfo.environmentStates.map { e ⇒
      scaladget.bootstrapnative.Row(Seq(
        e.taskName,
        CoreUtils.approximatedYearMonthDay(e.executionActivity.executionTime),
        displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize),
        displaySize(e.networkActivity.downloadedSize, e.networkActivity.readableDownloadedSize),
        e.submitted.toString,
        e.running.toString,
        e.done.toString,
        e.failed.toString
      ))
    },
    scaladget.bootstrapnative.BSTableStyle(bsn.bordered_table, tools.emptyMod))

  def render = div(
    jobViewButton.render,
    rxIf(panels.executionPanel.jobViews.map {
      _(executionId) == CapsuleView
    }, capsuleTable.render(width := "100="), environmentTable.render(width := "100%"))
  )
}