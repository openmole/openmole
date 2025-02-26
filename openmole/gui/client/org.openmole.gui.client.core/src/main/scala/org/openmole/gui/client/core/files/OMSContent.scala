package org.openmole.gui.client.core.files


import org.openmole.gui.client.core.{CoreFetch, ExecutionPanel, Panels, Waiter}
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.features.unitArrows
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.tool.{Component, OMTags}
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.ErrorData.stackTrace
import scaladget.bootstrapnative.bsn.*

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.ace.Editor

object OMSContent:

  def setError(safePath: SafePath, errorDataOption: Option[ErrorData])(using panels: Panels) =
    val editorPanelUI = panels.tabContent.editorPanelUI(safePath)
    editorPanelUI.foreach: pui=>
      pui.errors.set(errorDataOption)
      errorDataOption match 
        case Some(ed)=> panels.treeNodePanel.scriptErrors.update(se=> (se :+ pui).distinct)
        case _=> panels.treeNodePanel.scriptErrors.update(se=> se.filterNot(_ == pui))

  def buildTab(safePath: SafePath, initialContent: String, initialHash: String)(using panels: Panels, api: ServerAPI, path: BasePath, guiPlugins: GUIPlugins) =

    val editor = EditorPanelUI(safePath, initialContent, initialHash)
    val tabData = TabData(safePath, Some(editor))

    lazy val controlElement =
      val compileDisabled = Var(false)

      import scala.concurrent.duration._

      div(display.flex, flexDirection.row, height := "6vh", alignItems.center,
        child <-- compileDisabled.signal.map { compDisabled =>
          if compDisabled
          then Waiter.waiter()
          else
            div(display.flex, flexDirection.row,
              button("RUN", btn_primary, marginLeft := "10", onClick --> { _ =>
                editor.unsetErrors
                panels.tabContent.save(tabData, saveUnmodified = true).foreach: saved =>
                  if saved
                  then
                    api.launchScript(safePath, true).foreach: execID =>
                      panels.executionPanel.currentOpenSimulation.set(Some(execID))
                    ExecutionPanel.open
              }),
              button("CHECK", btn_secondary, marginLeft := "10", onClick --> { _ =>
                println("Check")
                editor.unsetErrors
                editor.editor.getSession().clearBreakpoints()
                compileDisabled.set(true)

                panels.tabContent.save(tabData, saveUnmodified = true).flatMap: saved =>
                  if saved
                  then
                    api.validateScript(safePath).map: errorDataOption =>
                      setError(safePath, errorDataOption)
                      editor.editor.focus()
                  else concurrent.Future.successful(())
                .onComplete: _ =>
                  compileDisabled.set(false)

              }),
              child <--
                (editor.errors.signal combineWith editor.errorMessage).map: (e, em) =>
                  if e.isDefined || em.isDefined
                  then button("CLEAR", btn_secondary, marginLeft := "10", onClick -->  
                    panels.treeNodePanel.clearErrorView(Some(safePath))
                  )
                  else div()
            )
        },
        div(row, marginLeft.auto)
      )

    val content = div(display.flex, flexDirection.column, controlElement, editor.view)

    (tabData, content)
