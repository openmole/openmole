package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.{Waiter, panels}
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data._
import com.raquo.laminar.api.L._
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.ext.client._
import scaladget.bootstrapnative.bsn._
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.core.Fetch

object OMSContent {
  def addTab(safePath: SafePath, initialContent: String, initialHash: String) = {

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
              TabContent.save(tabData, _ ⇒
              Fetch.future(_.compileScript(ScriptData(safePath)).future, timeout = 120 seconds, warningTimeout = 60 seconds).foreach { errorDataOption ⇒
                setError(errorDataOption)
                editor.editor.focus()
              }
              )
            })
        },
        div(display.flex, flexDirection.row,
          button("RUN", btn_primary_outline, cls := "omsControlButton", marginLeft := "10", onClick --> { _ ⇒
            unsetErrors
            TabContent.save(tabData, _ ⇒
              Fetch.future(_.runScript(ScriptData(safePath), true).future, timeout = 120 seconds, warningTimeout = 60 seconds).foreach { execInfo ⇒
                panels.openExecutionPanel
              }
            )
          })
        ),
        div(row, TabContent.fontSizeControl, marginLeft.auto)
      )
    }


    val content = div(display.flex, flexDirection.column, controlElement, editor.view)

    TabContent.addTab(tabData, content)
  }
}
