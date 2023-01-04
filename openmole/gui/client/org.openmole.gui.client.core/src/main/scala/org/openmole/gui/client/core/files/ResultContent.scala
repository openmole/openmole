package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.{Fetch, Panels}
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.core.files.TreeNodeTab.Raw

object ResultContent {

  sealed trait ResultView

  object Raw extends ResultView

  object Table extends ResultView

  object Plot extends ResultView

  def addTab(safePath: SafePath, initialContent: String, initialHash: String)(using panels: Panels, fetch: Fetch) = {

    val rowData: RowData = safePath.extension match {
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
      dataTable(rowData.content)
        .addHeaders(rowData.headers: _*)
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
        case _ => safePath.extension match {
          case FileExtension.CSV =>
            val columns = rowData.content.transpose
            //We only keep data of dimension 0 or 1
            val plotData = ColumnData(rowData.headers.zip(columns).zip(rowData.dimensions).flatMap { case ((h, c), d) =>
              d match {
                case 0 => Some(Column(h, ScalarColumn(c)))
                case 1 => Some(Column(h, ArrayColumn(c.map {
                  ResultData.fromStringToArray(_)
                })))
                case _ => None
              }
            })
            view.set(ResultPlot.fromCSV(plotData))
          case _ => div("plot OMR")
        }
      }
    }

    object DataDisplay
    val rawState = ToggleState(DataDisplay, "Raw", btn_primary_string, _ ⇒ switchView(Raw))
    val tableState = ToggleState(DataDisplay, "Table", btn_primary_string, _ ⇒ switchView(Table))
    val plotState = ToggleState(DataDisplay, "Plot", btn_primary_string, _ ⇒ switchView(Plot))


    val switchButton = exclusiveRadio(Seq(rawState, tableState, plotState), btn_secondary_string, 1).element
    val controlElement = div(switchButton)

    val content = div(display.flex, flexDirection.column, controlElement, child <-- view)

    panels.tabContent.addTab(tabData, content)

  }
}
