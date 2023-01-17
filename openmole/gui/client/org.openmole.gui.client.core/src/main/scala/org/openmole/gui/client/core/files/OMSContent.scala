package org.openmole.gui.client.core.files


import org.openmole.gui.client.core.{ExecutionPanel, Fetch, Panels, Waiter}
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import scaladget.bootstrapnative.bsn.*

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.ace.Editor

object OMSContent {

  def setError(safePath: SafePath, errorDataOption: Option[ErrorData])(using panels: Panels) = {

    val editorPanelUI = panels.tabContent.editorPanelUI(safePath)

    errorDataOption match {
      case Some(ce: CompilationErrorData) ⇒
        editorPanelUI match {
          case Some(ed: EditorPanelUI) =>
            ed.errors.set(EditorErrors(
              errorsFromCompiler = ce.errors.map { ewl ⇒
                ErrorFromCompiler(ewl, ewl.line.map { l ⇒
                  ed.editor.getSession().doc.getLine(l)
                }.getOrElse(""))
              },
              errorsInEditor = ce.errors.flatMap {
                _.line
              }
            )
            )
          case _ =>
        }
      case _ ⇒
    }
  }

  def addTab(safePath: SafePath, initialContent: String, initialHash: String)(using panels: Panels, fetch: Fetch, api: ServerAPI) = {

    val editor = EditorPanelUI(safePath.extension, initialContent, initialHash)
    val tabData = TabData(safePath, Some(editor))

    lazy val controlElement = {
      val compileDisabled = Var(false)

      def unsetErrors = {
        editor.errors.set(EditorErrors())
        editor.errorMessageOpen.set(false)
      }

      import scala.concurrent.duration._

      div(display.flex, flexDirection.row, height := "5vh", alignItems.center,
        child <-- compileDisabled.signal.map { compDisabled ⇒
          if (compDisabled) Waiter.waiter
          else
            button("CHECK", btn_secondary_outline, cls := "omsControlButton", onClick --> { _ ⇒
              unsetErrors
              editor.editor.getSession().clearBreakpoints()
              compileDisabled.set(true)

              panels.tabContent.save(tabData, _ ⇒
                fetch.future(_.compileScript(ScriptData(safePath)).future, timeout = 120 seconds, warningTimeout = 60 seconds).foreach { errorDataOption ⇒
                  compileDisabled.set(false)
                  setError(safePath, errorDataOption)
                  editor.editor.focus()
                }
              )
            })
        },
        div(display.flex, flexDirection.row,
          button("RUN", btn_primary_outline, cls := "omsControlButton", marginLeft := "10", onClick --> { _ ⇒
            unsetErrors
            panels.tabContent.save(tabData, _ ⇒
              fetch.future(_.runScript(ScriptData(safePath), true).future, timeout = 120 seconds, warningTimeout = 60 seconds).foreach { execInfo ⇒
                ExecutionPanel.open
              }
            )
          })
        ),
        div(row, panels.tabContent.fontSizeControl, marginLeft.auto)
      )
    }


    val content = div(display.flex, flexDirection.column, controlElement, editor.view)

    panels.tabContent.addTab(tabData, content)
  }
}
