package org.openmole.gui.client.core.files


import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import scaladget.nouislider.*

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.nouislider.NoUISliderImplicits.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.{CoreFetch, Panels}
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.shared.data.GUIVariable.ValueType
import org.openmole.gui.shared.data.GUIVariable.ValueType.unwrap


object PlotContent:

  case class PlotContentSection(section: String, rawContent: String, rowData: RowData, initialHash: String)

  enum ResultView:
    case Raw, Table, Plot, Metadata, HistoryMetadata

  case class Section(name: String)

  case class RawTablePlot(editor: EditorPanelUI, table: HtmlElement, plot: HtmlElement, metadata: HtmlElement)

  case class ResultViewAndSection(resultView: ResultView, historyView: Option[ResultView], section: Option[Section])

  case class OMRMetadata(script: HtmlElement, openmoleVersion: String, timeStart: Long, history: Boolean)

  def buildTab(
                safePath: SafePath,
                extension: FileContentType,
                sections: Seq[PlotContentSection],
                omrMetadata: Option[OMRMetadata] = None,
                currentIndex: Option[Int] = None)(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins): (TabData, HtmlElement) =
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
          dataTable(s.rowData.content.map(_.map(RowData.toDataContent)))
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
              case ((h, c), d) if d == 0 => Some(Column(h, ScalarColumn(c.map(_.value))))
              case ((h, c), d) if d == 1 => Some(Column(h, ArrayColumn(c.map(_.value).map(ResultData.fromStringToArray))))
              case _ => None
          )
          ResultPlot.fromColumnData(plotData)

        val metadataHistory =
          val sliderValueText = Var("")
          div(
            child <--
              Signal.fromFuture(api.omrDataIndex(safePath)).map: dataIndex =>
                def valueTypeToInt(vt: ValueType): Int = unwrap(vt).toString.toInt
                def sliderValueToInt(s: Any) = s.toString.toDouble.toInt

                val dIndexValues = dataIndex.map(d => d.map(_.values)).getOrElse(Seq()).flatten
                val indexMap = dIndexValues.zipWithIndex.map(x => x._2 -> valueTypeToInt(x._1)).toMap

                val first2 = dIndexValues.slice(0, 2)

                val sortedKeys = indexMap.keys.toSeq.sorted
                val maxIndex = sortedKeys.last
                sliderValueText.set(indexMap(currentIndex.map(_.toDouble).getOrElse(maxIndex).toString.toInt).toString)

                val element = div(width := "500", marginRight := "50", marginTop := "20")
                noUiSlider.create(
                  element.ref, Options
                    .range(Range.min(sortedKeys.head.toDouble).max(maxIndex.toDouble))
                    .start(currentIndex.map(_.toDouble).getOrElse(maxIndex.toDouble))
                    .step(1.0)
                    .connect(Options.Lower)
                    .tooltips(false)
                )

                element.noUiSlider.on(event.ChangeEvent, (value, handle) =>
                  sliderValueText.set(indexMap(sliderValueToInt(value)).toString)
                )

                val sliderValueDiv = div(
                  child <--
                    sliderValueText.signal.map(t=>
                      div(t, marginRight := "20", fontSize := "18", color := "#3086b5", fontWeight.bold)
                    )
                )

                val loadDataButton =
                  button(
                    btn_primary, "Load data",
                    onClick --> { _ =>
                      val newCurrentIndex = sliderValueToInt(element.ref.noUiSlider.get())

                      val dataFile =
                        dataIndex flatMap: dis=>
                          (dis.map : di=>
                            di.fileIndex(newCurrentIndex)).headOption

                      api.omrContent(safePath, dataFile).map: guiContent =>
                        panels.tabContent.removeTab(safePath)
                        val (tabData, content) = OMRContent.buildTab(safePath, guiContent, Some(newCurrentIndex.toInt))
                        panels.tabContent.addTab(tabData, content)
                    })
                div(flexRow, alignItems.end,sliderValueDiv, element, loadDataButton)
          )

        val metadata =
          omrMetadata match
            case Some(md) =>
              div(
                cls := "metadata",
                div(display.flex, flexDirection.row, span("OpenMOLE Version:", nbsp, fontWeight.bold), md.openmoleVersion),
                div(display.flex, flexDirection.row, marginTop := "20", span("Launched:", nbsp, fontWeight.bold), CoreUtils.longTimeToString(md.timeStart)),
                div(display.flex, flexDirection.column, marginTop := "20", span("Exploration history:", nbsp, fontWeight.bold), metadataHistory),
                div("Script: ", fontWeight.bold, marginTop := "20", marginBottom := "10"),
                div(fontFamily := "monospace", fontSize := "medium", cls := "execTextArea", overflow := "scroll", margin := "10px", md.script),
                if md.history
                then div(
                  fileActions, backgroundColor := "white", width := "800px",
                  a(div(fileActionItems, FileToolBox.glyphItemize(glyph_download), "JSON History"), href := org.openmole.gui.shared.api.convertOMR(safePath, GUIOMRContent.ExportFormat.JSON, true)),
                  a(div(fileActionItems, marginLeft := "20px", FileToolBox.glyphItemize(glyph_download), "CSV History"), href := org.openmole.gui.shared.api.convertOMR(safePath, GUIOMRContent.ExportFormat.CSV, true))
                )
                else div()
                //textArea(md.script, idAttr := "execTextArea", fontFamily := "monospace", fontSize := "medium", height := "400", width := "100%", readOnly := true)
              )
            case _ => div("Unavailable metadata")

        s.section -> RawTablePlot(editor, table, plot, metadata)
        ).toMap

    val currentResultViewAndSection: Var[ResultViewAndSection] = Var(ResultViewAndSection(ResultView.Table, None, sectionMap.keys.headOption.map(s => Section(s))))

    val tabData = TabData(safePath, None)

    def switchView(resultView: ResultView): Unit = currentResultViewAndSection.update(rvs => rvs.copy(resultView = resultView))

    def switchSection(section: Section): Unit = currentResultViewAndSection.update(rvs => rvs.copy(section = Some(section)))

    def rawTablePlot(resultViewAndSection: ResultViewAndSection) =
      resultViewAndSection.section match
        case Some(s: Section) => sectionMap(s.name)
        case _ => sectionMap.values.head

    def toView(resultViewAndSection: ResultViewAndSection) =
      val rTP = rawTablePlot(resultViewAndSection)

      resultViewAndSection.resultView match
        case Raw => rTP.editor.view
        case Table => rTP.table
        case Plot => rTP.plot
        case Metadata => rTP.metadata
        case _ => div()

    val rawState = ToggleState(ResultView, "CSV", btn_primary_string, _ ⇒ switchView(ResultView.Raw))
    val tableState = ToggleState(ResultView, "Table", btn_primary_string, _ ⇒ switchView(ResultView.Table))
    val plotState = ToggleState(ResultView, "Plot", btn_primary_string, _ ⇒ switchView(ResultView.Plot))
    val metadataState = ToggleState(ResultView, "More", btn_primary_string, _ ⇒ switchView(ResultView.Metadata))
    val switchButton = exclusiveRadio(Seq(tableState, plotState, rawState, metadataState), btn_secondary_string, 0)

    val sectionStates: Seq[ToggleState[Section]] =
      sectionMap.keys.map(name =>
        val section = Section(name)
        ToggleState(Section(name), name, s"btn ${btn_success_string}", _ => switchSection(section))
      ).toSeq
    lazy val sectionSwitchButton = exclusiveRadio(sectionStates, btn_secondary_string, 0)

    val content =
      div(display.flex, flexDirection.row,
        div(display.flex, flexDirection.column, width := "100%",
          div(display.flex, flexDirection.row,
            (sectionStates.size match
              case 1 => div()
              case _ => sectionSwitchButton.element.amend(margin := "10", width := "150px")),
            switchButton.element.amend(margin := "10", width := "150px", marginLeft := "50")
          ),
          child <-- currentResultViewAndSection.signal.map(rvs => toView(rvs))
        )
      )

    (tabData, content)



