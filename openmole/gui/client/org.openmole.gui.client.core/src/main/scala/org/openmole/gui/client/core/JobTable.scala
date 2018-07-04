//package org.openmole.gui.client.core
//
//import org.openmole.gui.client.core.ExecutionPanel.{ CapsuleView, EnvironmentView, JobView }
//import org.openmole.gui.ext.data._
//import org.openmole.gui.ext.tool.client.omsheet
//import scaladget.bootstrapnative.bsn
//import bsn._
//import scaladget.tools
//import scaladget.tools._
//import scalatags.JsDom.all._
//import rx._
//import scaladget.bootstrapnative.DataTable.DataRow
//import scaladget.bootstrapnative.Table.SubRow
//
//object JobTable {
//  def apply(executionId: ExecutionId) = new JobTable(executionId)
//
//  private def displaySize(size: Long, readable: String) =
//    if (size == 0L) ""
//    else s"($readable)"
//
//  //val expandedErrors: Var[Map[EnvironmentId, Boolean]] = Var(Map())
//}
//
//import JobTable._
//
//class JobTable(executionId: ExecutionId) {
//
//  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
//
//  val jobViews: Var[JobView] = Var(CapsuleView)
//
//  val capsuleActivated = jobViews.now == CapsuleView
//
//  val jobViewButton = bsn.radios(omsheet.centerElement +++ Seq(marginTop := 15, marginBottom := 20))(
//    bsn.selectableButton("Capsules", capsuleActivated, onclick = () ⇒ switchView),
//    bsn.selectableButton("Environments", !capsuleActivated, onclick = () ⇒ switchView)
//  )
//
//  val envErrors: Var[Map[EnvironmentId, EnvironmentErrorData]] = Var(Map())
//  val environmentStates: Var[Seq[Var[EnvironmentState]]] = Var(Seq())
//
//  val errOpen = Var(false)
//  panels.executionPanel.envError.trigger {
//    println("TRRRRIGER " + errOpen.now)
//    if (!errOpen.now) {
//      println("--------------------- Update errors")
//      envErrors() = panels.executionPanel.envError.now
//    }
//  }
//
//  def switchView = {
//    jobViews() = jobViews.now match {
//      case EnvironmentView ⇒ CapsuleView
//      case _               ⇒ EnvironmentView
//    }
//  }
//
//  //val executionInfo: Var[ExecutionInfo] = Var(initExecInfo)
//
//  //  def delay: SetTimeoutHandle = {
//  //    timers.setTimeout(8000) {
//  //      println("Update envs")
//  //      panels.executionPanel.execInfo.now.executionInfos.filter(_._1 == executionId).map {
//  //        _._2
//  //      }.headOption.foreach { e ⇒
//  //        executionInfo() = e
//  //      }
//  //      delay
//  //    }
//  //  }
//
////  def execInfos = panels.executionPanel.executionInfo.map {
////    _.executionInfos.filter(_._1 == executionId).headOption
////  }
////
////  val updateEnvironmentStates = execInfos.map {
////    _.map {
////      _._2.environmentStates
////    }.getOrElse(Seq())
////  }
//
//  val environmentStates = panels.executionPanel.executionInfo.map{ei=>
//    ei.get(executionId).map{_.environmentStates.map{_.}}
//  }
//
//  def capsuleTable(info: ExecutionInfo) = {
//    scaladget.bootstrapnative.DataTable(
//      Some(scaladget.bootstrapnative.Table.Header(Seq("Name", "Running", "Completed"))),
//      info.capsules.map { c ⇒ scaladget.bootstrapnative.DataTable.DataRow(Seq(c._1.toString, c._2.running.toString, c._2.completed.toString)) },
//      scaladget.bootstrapnative.Table.BSTableStyle(bsn.bordered_table, tools.emptyMod), true).render(width := "100=").render
//  }
//
//  def error(errors: Map[EnvironmentId, EnvironmentErrorData], environmentId: EnvironmentId) = errors.getOrElse(environmentId, EnvironmentErrorData.empty)
//
//  val environmentTable /*(envS: Seq[EnvironmentState], errors: Map[EnvironmentId, EnvironmentErrorData])*/ = {
//    {
//      scaladget.bootstrapnative.Table(
//        Some(scaladget.bootstrapnative.Table.Header(Seq("Name", "Elapsed time", "Uploads", "Downloads", "Submitted", "Running", "Finished", "Failed", "Errors", "Actions"))),
//        environmentStates.now.map { ee ⇒
//          scaladget.bootstrapnative.Table.ReactiveRow(
//            ee.map { e ⇒
//              Seq(
//                span(e.taskName),
//                span(CoreUtils.approximatedYearMonthDay(e.executionActivity.executionTime)),
//                span(displaySize(e.networkActivity.uploadedSize, e.networkActivity.readableUploadedSize)),
//                span(displaySize(e.networkActivity.downloadedSize, e.networkActivity.readableDownloadedSize)),
//                span(e.submitted.toString),
//                span(e.running.toString),
//                span(e.done.toString),
//                span(e.failed.toString),
//                if (e.numberOfErrors == 0) span() else
//                  button("Errors", onclick := { () ⇒
//                    errOpen() = !errOpen.now
//                    //  val previousState = expandedErrors.now.get(e.envId).getOrElse(false)
//                    //   expandedErrors() = expandedErrors.now.updated(e.envId, !previousState)
//                    // panels.executionPanel.updateEnvErrors(e.envId)
//                    //   panels.executionPanel.toggleEnvironmentErrorPanel(e.envId)
//                  }, bsn.btn_danger)(badge(e.numberOfErrors.toString)),
//                // span(bsn.buttonGroup(omsheet.columnLayout +++ (width := 80))(
//                // bsn.buttonIcon(glyphicon = bsn.glyph_refresh, todo = { () ⇒ panels.executionPanel.updateEnvErrors(e.envId) }).tooltip("Refresh environment errors"),
//                bsn.buttonIcon(buttonStyle = bsn.btn_default, glyphicon = bsn.glyph_repeat, todo = () ⇒ panels.executionPanel.clearEnvErrors(e.envId) /*).tooltip("Reset environment errors")*/ )
//              )
//            },
//            subRow = Some(SubRow({
//              val panel = new EnvironmentErrorPanel
//              div(
//                envErrors.map { envErrors ⇒
//                  panel.setErrors(error(envErrors, ee.now.envId))
//                  panel.view
//                })
//            }, errOpen)))
//        },
//
//        //              div(panels.executionPanel.staticPanel(e.envId, panels.executionPanel.envErrorPanels,
//        //                () ⇒ new EnvironmentErrorPanel,
//        //                (ep: EnvironmentErrorPanel) ⇒
//        //                  ep.setErrors(error(errors, e.envId))).view)
//        //  ,
//        // errOpen
//        //                {
//        //                  println("EXPERROR " + expandedErrors)
//        //                  expandedErrors.map {
//        //                    _.getOrElse(e.envId, false)
//        //                  }
//        //                }
//
//        scaladget.bootstrapnative.Table.BSTableStyle(bsn.bordered_table, tools.emptyMod)
//      ).render(minWidth := 1000)
//    }
//  }
//
//  def render = {
//    div(
//      Rx {
//        if (environmentStates().isEmpty) div("Pleeeease waaiit")
//        else {
//          div(
//            jobViewButton.render,
//            if (jobViews() == CapsuleView) {
//              execInfos() match {
//                case Some((_, info)) ⇒ capsuleTable(info)
//                case _               ⇒ println("wait capsules")
//              }
//
//            }
//            else {
//              environmentStates() match {
//                case states: Seq[EnvironmentState] ⇒ environmentTable(states, envErrors())
//                case _                             ⇒ div("wait ENOVSS")
//              }
//            }
//          )
//        }
//      }
//    )
//  }
//
//}