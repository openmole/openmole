package org.openmole.gui.client.core.files


import org.openmole.gui.client.core.{ExecutionPanel, Panels, Waiter}
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.features.unitArrows
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.tool.ace.Editor
import org.openmole.gui.client.tool.{Component, OMTags}
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.ErrorData.stackTrace
import org.openmole.gui.client.tool.bootstrapnative.bsn.*
import io.github.nguyenyou.webawesome.laminar.*

import scala.concurrent.ExecutionContext.Implicits.global

object OMSContent:

  def buildTab(safePath: SafePath, initialContent: String, initialHash: String)(using panels: Panels, api: ServerAPI, path: BasePath, guiPlugins: GUIPlugins) =

    val editor = EditorPanelUI(safePath, initialContent, initialHash)
    val tabData = TabData(safePath, Some(editor))

    lazy val controlElement =
      val compileDisabled = Var(false)

      import scala.concurrent.duration._

      ButtonGroup(_.label := "OMSControl")(
        Button(
          _.cls := "purple",
          _.onClick --> { _ =>
            editor.unsetErrors
            panels.tabContent.save(tabData, saveUnmodified = true).foreach: saved =>
              if saved
              then
                api.launchScript(safePath).foreach: execID =>
                  panels.executionPanel.currentOpenSimulation.set(Some(execID))
                ExecutionPanel.open
          })("Run"),
        Button(
          _.appearance.outlined,
          _.cls := "buttonWhite",
          _.loading <-- compileDisabled,
          _.onClick --> { _ =>
            editor.unsetErrors
            editor.editor.getSession().clearBreakpoints()
            compileDisabled.set(true)

            panels.tabContent.save(tabData, saveUnmodified = true).flatMap: saved =>
              if saved
              then
                api.validateScript(safePath).map: errorDataOption =>
                  editor.errors.set(errorDataOption)
                  //setError(safePath, errorDataOption)
                  editor.editor.focus()
              else concurrent.Future.successful(())
            .onComplete: _ =>
              compileDisabled.set(false)

          })("Check"),
        child <--
          (editor.errors.signal combineWith editor.errorMessage).map: (e, em) =>
            if e.isDefined || em.isDefined
            then Button(
              _.appearance.outlined,
              _.cls := "buttonWhite",
              _.variant.neutral,
              _.onClick -->
                panels.treeNodePanel.clearErrorView(Some(safePath))
            )("Clear")
            else emptyNode
      )

    //            div(display.flex, flexDirection.row,
    //              button("RUN", btn_primary, marginLeft := "10",
    //
    //              button("CHECK", btn_secondary, marginLeft := "10", ),
    //              child <--
    //                (editor.errors.signal combineWith editor.errorMessage).map: (e, em) =>
    //                  if e.isDefined || em.isDefined
    //                  then button("CLEAR", btn_secondary, marginLeft := "10", onClick -->
    //                    panels.treeNodePanel.clearErrorView(Some(safePath))
    //                  )
    //                  else div()

    // div(row, marginLeft.auto)

    val content = div(display.flex, flexDirection.column, editor.view)

    (tabData, content, controlElement)
