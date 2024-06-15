package org.openmole.gui.client.core.files


import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.{CoreFetch, Panels}
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.core.CoreUtils


object PlotContent:

  case class PlotContentSection(section: String, rawContent: String, rowData: RowData, initialHash: String)

  enum ResultView:
    case Raw, Table, Plot, Metadata

  case class Section(name: String)

  case class RawTablePlot(editor: EditorPanelUI, table: HtmlElement, plot: HtmlElement, metadata: HtmlElement)

  case class ResultViewAndSection(resultView: ResultView, section: Option[Section])

  case class OMRMetadata(script: HtmlElement, openmoleVersion: String, timeStart: Long)

  def buildTab(
    safePath: SafePath,
    extension: FileContentType,
    sections: Seq[PlotContentSection],
    omrMetadata: Option[OMRMetadata] = None)(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins) =
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
          val plotData = ColumnData(
            s.rowData.headers.zip(columns).zip(s.rowData.dimensions).flatMap:
              case ((h, c), d) if d == 0 => Some(Column(h, ScalarColumn(c)))
              case ((h, c), d) if d == 1 => Some(Column(h, ArrayColumn(c.map(ResultData.fromStringToArray))))
              case _ => None
          )
          ResultPlot.fromColumnData(plotData)

        val metadata =
          omrMetadata match
            case Some(md) =>
              div(
                cls := "metadata",
                div(display.flex, flexDirection.row, span("OpenMOLE Version:", nbsp, fontWeight.bold), md.openmoleVersion),
                div(display.flex, flexDirection.row, marginTop := "10", span("Launched:", nbsp, fontWeight.bold), CoreUtils.longTimeToString(md.timeStart)),
                div("Script: ", fontWeight.bold, marginTop := "10", marginBottom := "10"),
                div(fontFamily := "monospace", fontSize := "medium", cls := "execTextArea", overflow := "scroll", margin := "10px", md.script)
                //textArea(md.script, idAttr := "execTextArea", fontFamily := "monospace", fontSize := "medium", height := "400", width := "100%", readOnly := true)
              )
            case _=> div("Unavailable metadata")

        s.section -> RawTablePlot(editor, table, plot, metadata)
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
        case Metadata=> rawTablePlot.metadata

    val rawState = ToggleState(ResultView, "CSV", btn_primary_string, _ ⇒ switchView(ResultView.Raw))
    val tableState = ToggleState(ResultView, "Table", btn_primary_string, _ ⇒ switchView(ResultView.Table))
    val plotState = ToggleState(ResultView, "Plot", btn_primary_string, _ ⇒ switchView(ResultView.Plot))
    val metadataState = ToggleState(ResultView, "Info", btn_primary_string, _ ⇒ switchView(ResultView.Metadata))
    val switchButton = exclusiveRadio(Seq(tableState, plotState, rawState, metadataState), btn_secondary_string, 0)

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

    (tabData, content)



