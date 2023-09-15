package org.openmole.gui.client.core.files


import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.{CoreFetch, Panels}
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*


object PlotContent:

  sealed trait ResultView

  object Raw extends ResultView

  object Table extends ResultView

  object Plot extends ResultView

  def addTab( safePath: SafePath, rowData: RowData, initialContent: String, initialHash: String, extension: FileContentType)(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins) = {

    val editor = EditorPanelUI(extension, initialContent, initialHash)
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

    def switchView(resultView: ResultView) =
      resultView match
        case Raw => view.set(editor.view)
        case Table => view.set(table)
        case _ =>
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
          view.set(ResultPlot.fromColumnData(plotData))


    object DataDisplay
    val rawState = ToggleState(DataDisplay, "Raw", btn_primary_string, _ ⇒ switchView(Raw))
    val tableState = ToggleState(DataDisplay, "Table", btn_primary_string, _ ⇒ switchView(Table))
    val plotState = ToggleState(DataDisplay, "Plot", btn_primary_string, _ ⇒ switchView(Plot))


    val switchButton = exclusiveRadio(Seq(rawState, tableState, plotState), btn_secondary_string, 1)
      .element
      .amend(margin := "10", width := "150px")

    val content = div(display.flex, flexDirection.column, switchButton, child <-- view)

    panels.tabContent.addTab(tabData, content)

  }


