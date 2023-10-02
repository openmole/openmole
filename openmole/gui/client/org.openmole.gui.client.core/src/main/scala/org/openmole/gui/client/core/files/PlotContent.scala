package org.openmole.gui.client.core.files


import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.PlotContent.ResultView.{Plot, Raw, Table}
import org.openmole.gui.client.core.{CoreFetch, Panels}
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*


object PlotContent:

  case class PlotContentSection(section: String, rawContent: String, rowData: RowData, initialHash: String)

  enum ResultView:
    case Raw, Table, Plot

  case class Section(name: String)

  case class RawTablePlot(editor: EditorPanelUI, table: HtmlElement, plot: HtmlElement)

  case class ResultViewAndSection(resultView: ResultView, section: Option[Section])

  def addTab(safePath: SafePath, extension: FileContentType, sections: Seq[PlotContentSection])(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins) = {

    import ResultView.*
    val sectionMap =
      (sections.map: s =>
        val editor = EditorPanelUI(extension, s.rawContent, "initialHash")
        editor.setReadOnly(true)

        // FIXME: this should be in css but for some reason it does not work this way
        val headerStyle = Seq(position := "sticky",
          top := "0",
          background := "#dbdbdb",
          color := "#3f3d56"
        )

        val table = div(idAttr := "editor",
          dataTable(s.rowData.content)
            .addHeaders(s.rowData.headers: _*)
            .style(tableStyle = Seq(bordered_table), headerStyle = headerStyle)
            .sortable
            .render.render.amend(borderCollapse.collapse)
        )

        val plot =
          val columns = s.rowData.content.transpose
          //We only keep data of dimension 0 or 1
          val plotData = ColumnData(s.rowData.headers.zip(columns).zip(s.rowData.dimensions).flatMap { case ((h, c), d) =>
            d match {
              case 0 => Some(Column(h, ScalarColumn(c)))
              case 1 => Some(Column(h, ArrayColumn(c.map {
                ResultData.fromStringToArray(_)
              })))
              case _ => None
            }
          })
          ResultPlot.fromColumnData(plotData)

        s.section -> RawTablePlot(editor, table, plot)
        ).toMap

    val currentResultViewAndSection: Var[ResultViewAndSection] = Var(ResultViewAndSection(ResultView.Table, sectionMap.keys.headOption.map(s => Section(s))))

    val tabData = TabData(safePath, None)

    def switchView(resultView: ResultView): Unit = currentResultViewAndSection.update(rvs => rvs.copy(resultView = resultView))

    def switchSection(section: Section): Unit = currentResultViewAndSection.update(rvs => rvs.copy(section = Some(section)))

    def toView(resultViewAndSection: ResultViewAndSection) =
      val rawTablePlot =
        resultViewAndSection.section match
          case Some(s: Section) => sectionMap(s.name)
          case _ => sectionMap.values.head

      resultViewAndSection.resultView match
        case Raw => rawTablePlot.editor.view
        case Table => rawTablePlot.table
        case Plot => rawTablePlot.plot
        case _ => div()

    val rawState = ToggleState(ResultView, "Raw", btn_primary_string, _ ⇒ switchView(ResultView.Raw))
    val tableState = ToggleState(ResultView, "Table", btn_primary_string, _ ⇒ switchView(ResultView.Table))
    val plotState = ToggleState(ResultView, "Plot", btn_primary_string, _ ⇒ switchView(ResultView.Plot))
    val switchButton = exclusiveRadio(Seq(rawState, tableState, plotState), btn_secondary_string, 1)

    val sectionStates: Seq[ToggleState[Section]] =
      sectionMap.keys.map(name =>
        val section = Section(name)
        ToggleState(Section(name), name, s"btn ${btn_success_string}", _ => switchSection(section))
      ).toSeq
    lazy val sectionSwitchButton = exclusiveRadio(sectionStates, btn_secondary_string, 0)

    val content = div(display.flex, flexDirection.column,
      div(display.flex, flexDirection.row,
        (sectionStates.size match
          case 1 => div()
          case _ => sectionSwitchButton.element.amend(margin := "10", width := "150px")),
        switchButton.element.amend(margin := "10", width := "150px", marginLeft := "50")
      ),
      child <-- currentResultViewAndSection.signal.map(rvs => toView(rvs))
    )

    panels.tabContent.addTab(tabData, content)

  }


