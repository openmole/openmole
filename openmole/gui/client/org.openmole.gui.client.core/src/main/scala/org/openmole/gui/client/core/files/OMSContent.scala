package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.{ExecutionPanel, Fetch, Panels, Waiter}
import org.openmole.gui.ext.data.*
import org.openmole.gui.ext.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.ext.client.*
import scaladget.bootstrapnative.bsn.*

import scala.concurrent.ExecutionContext.Implicits.global

object OMSContent {
  def addTab(safePath: SafePath, initialContent: String, initialHash: String)(using panels: Panels, fetch: Fetch) = {

    val editor = EditorPanelUI(safePath.extension, initialContent, initialHash)
    val tabData = TabData(safePath, Some(editor))

    lazy val controlElement = {
      val compileDisabled = Var(false)

      def unsetErrors = {
        editor.errors.set(EditorErrors())
        editor.errorMessageOpen.set(false)
      }

      def setError(errorDataOption: Option[ErrorData]) = {
        compileDisabled.set(false)
        errorDataOption match {
          case Some(ce: CompilationErrorData) ⇒
            editor.errors.set(EditorErrors(
              errorsFromCompiler = ce.errors.map { ewl ⇒
                ErrorFromCompiler(ewl, ewl.line.map { l ⇒
                  editor.editor.getSession().doc.getLine(l)
                }.getOrElse(""))
              },
              errorsInEditor = ce.errors.flatMap {
                _.line
              }
            )
            )
          case _ ⇒
        }
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
                setError(errorDataOption)
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
                ExecutionPanel.open(panels.executionPanel, panels.bannerAlert)
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
