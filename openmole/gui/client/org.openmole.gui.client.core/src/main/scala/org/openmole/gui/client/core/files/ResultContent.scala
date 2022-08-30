package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._
import scaladget.bootstrapnative.bsn._
import com.raquo.laminar.api.L._
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.core.files.TreeNodeTab.Raw

object ResultContent {

  sealed trait ResultView

  object Raw extends ResultView

  object Table extends ResultView

  object Plot extends ResultView

  def addTab(safePath: SafePath, initialContent: String, initialHash: String) = {

    val resultData: ResultData = safePath.extension match {
      case FileExtension.CSV => ResultData.fromCSV(initialContent)
    }

    val editor = EditorPanelUI(safePath.extension, initialContent, initialHash)
    editor.setReadOnly(true)

    // FIXME: this should be in css but for some reason it does not work this way
    val headerStyle = Seq(position := "sticky",
      top := "0",
      background := "#dbdbdb",
      color := "#3f3d56"
    )

    val table = div(idAttr := "editor",
      dataTable(resultData.content)
        .addHeaders(resultData.header: _*)
        .style(tableStyle = Seq(bordered_table), headerStyle = headerStyle)
        .sortable
        .render.render.amend(borderCollapse.collapse)
    )

    val tabData = TabData(safePath, Some(editor))
    val view: Var[HtmlElement] = Var(table)

    def switchView(resultView: ResultView) = {
      resultView match {
        case Raw => view.set(editor.view)
        case Table => view.set(table)
        case _ => view.set(div("plot"))
      }
    }

    val rawState = ToggleState("Raw", btn_primary_string, () ⇒ switchView(Raw))
    val tableState = ToggleState("Table", btn_primary_string, () ⇒ switchView(Table))
    val plotState = ToggleState("Plot", btn_primary_string, () ⇒ switchView(Plot))


    val switchButton = exclusiveRadio(Seq(rawState, tableState, plotState), btn_secondary_string, tableState)
    val controlElement = div(switchButton)

    val content = div(display.flex, flexDirection.column, controlElement, child <-- view)

    TabContent.addTab(tabData, content)

  }
}
