package org.openmole.gui.client.core.files


import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import scaladget.nouislider.*

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.nouislider.NoUISliderImplicits.*
import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.features.unitArrows
import org.openmole.gui.client.core.{CoreFetch, Panels}
import org.openmole.gui.client.core.files.TabContent.TabData
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.shared.data.GUIVariable.ValueType
import org.openmole.gui.shared.data.GUIVariable.ValueType.unwrap
import org.openmole.gui.client.tool.OMTags.btn_purple
import org.openmole.gui.client.core.Waiter
import org.openmole.gui.client.tool.plot.Plot.NumberOfColumToBePlotted
import org.openmole.gui.client.tool.Component
import scala.scalajs.js.timers
import scalaz.Alpha.C
import org.checkerframework.checker.units.qual.m


object PlotContent:

  enum ResultView:
    case Raw, Table, Plot, Metadata
  
  import ResultView._  
  trait ContentState

  case class TableState(scrollDown: Boolean = false, resultView: ResultView = Table) extends ContentState
  case class RawState(scrollDown: Boolean = false, resultView: ResultView = Raw) extends ContentState
  case class MetadataState(resultView: ResultView = Metadata) extends ContentState
  case class PlotState(
    numberOfColumToBePlotted: NumberOfColumToBePlotted = NumberOfColumToBePlotted.One, 
    initialHeaders: Seq[String] = Seq()
    ) extends ContentState

  case class ContentStates(
    table: TableState = TableState(),
    raw: RawState = RawState(),
    plot: PlotState = PlotState()
  )
  object ContentState:
    def fromResultView(resultView: ResultView, contentStates: ContentStates) =  
      resultView match
        case Table=> contentStates.table
        case Raw => contentStates.raw
        case Plot => contentStates.plot
        case Metadata => MetadataState()

  implicit class ContentStatesW(cs: ContentStates):
    def update(contentState: ContentState) = 
      contentState match
        case s: TableState => cs.copy(table = s)
        case r: RawState => cs.copy(raw = r)
        case p: PlotState => cs.copy(plot = p)
        case _=> cs

  case class ContentSection(section: String, rawContent: String, rowData: RowData, initialHash: String, historyView: Option[ResultView] = None)

  case class Section(name: String)

  //case class RawTablePlot(editor: EditorPanelUI, table: HtmlElement, plot: HtmlElement, resultPlot: ResultPlot, metadata: HtmlElement)

  //case class StateAndSection(contentState: ContentState, contentSection: ContentSection, historyView: Option[ResultView])

  case class OMRMetadata(script: HtmlElement, openmoleVersion: String, timeStart: Long, history: Boolean)

  trait SectionView:
    def view: HtmlElement
  case class BasicView(view: HtmlElement) extends SectionView
  case class EditorView(view: HtmlElement, editor: EditorPanelUI) extends SectionView
  case class PlotView(view: HtmlElement, resultPlot: ResultPlot) extends SectionView

  def buildTab(
    safePath: SafePath,
    extension: FileContentType,
    contentSections: Seq[ContentSection],
    currentSection: String,
    states: ContentStates = ContentStates(),
    currentState: ContentState = TableState(),
    omrMetadata: Option[OMRMetadata] = None,
    currentIndex: Option[Int] = None)(using panels: Panels, api: ServerAPI, basePath: BasePath, guiPlugins: GUIPlugins): (TabData, HtmlElement) =
    import ResultView.*

    def buildSectionView(sectionName: String) =
      contentSections.find(_.section == sectionName) match
        case Some(cs: ContentSection) =>
          currentState match
            case RawState(scrollDown, resultView) => 
              val editor = EditorPanelUI(safePath, cs.rawContent, "initialHash")
              editor.setReadOnly(true)
              EditorView(editor.view, editor)
            case TableState(scrollDown, resultView) => 
               val headerStyle = Seq(position := "sticky",
                  top := "0",
                  background := "#dbdbdb",
                  color := "#3f3d56"
                )

                BasicView(
                  div(
                    idAttr := "editor",
                    dataTable(cs.rowData.content.map(_.map(RowData.toDataContent)))
                    .addHeaders(cs.rowData.headers *)
                      .style(tableStyle = Seq(bordered_table), headerStyle = headerStyle)
                      .sortable
                      .render.render.amend(borderCollapse.collapse)
                  )
                )
            case ps: PlotState=>
              val columns = cs.rowData.content.transpose
              val plotData = ColumnData(
                  cs.rowData.headers.zip(columns).zip(cs.rowData.dimensions).flatMap:
                    case ((h, c), d) if d == 0 => Some(Column(h, ScalarColumn(c.map(_.value))))
                    case ((h, c), d) if d == 1 => Some(Column(h, ArrayColumn(c.map(_.value).map(ResultData.fromStringToArray))))
                    case _ => None
                )
              val resPlot = new ResultPlot(plotData, ps)
              val fcd = resPlot.fromColumnData 

              //We only keep data of dimension 0 or 1  
              PlotView(fcd, resPlot)
            case ms: MetadataState =>
              val metadataHistory =
                div(
                  child <--
                    Signal.fromFuture(api.omrDataIndex(safePath)).map: dataIndex =>
                      def valueTypeToInt(vt: ValueType): Int = unwrap(vt).toString.toInt
                      def sliderValueToInt(s: Any) = s.toString.toDouble.toInt

                      val dIndexValues = dataIndex.map(d => d.head.values).getOrElse(Seq())

                      if dIndexValues.size > 1
                      then
                        val sliderValueText = Var("")

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
                                val (tabData, content) = OMRContent.buildTab(safePath, guiContent, currentIndex = Some(newCurrentIndex.toInt))
                                panels.tabContent.addTab(tabData, content)
                            })
                        div(flexRow, alignItems.end,sliderValueDiv, element, loadDataButton)
                      else div()
                )

              BasicView(
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
                )
        case _=> BasicView(div())

    val sectionView = buildSectionView(currentSection) 

    val tabData = TabData(safePath, None)    

    def setScrollToBottom: Unit =
      sectionView match
          case EditorView(_, editor) => editor.scrollToBottom
          case BasicView(view) => view.ref.scrollTop = view.ref.scrollHeight
          case _ =>

    def updatedContentState = 
      currentState match
        case _: TableState => TableState()
        case _: RawState => RawState()
        case _: PlotState =>
            sectionView match
              case PlotView(view, resultPlot) => 
                val selectedAxis = resultPlot.axisRadios.now().selected.now().map(_.t)
                val numberOfColumToBePlotted = resultPlot.oneTwoNRadio.selected.now().map(x=> x.t).head
                PlotState(numberOfColumToBePlotted, selectedAxis)
              case _ => PlotState(NumberOfColumToBePlotted.One, Seq())
        case _: MetadataState => MetadataState()

    def switchFromState(state: ContentState, states: ContentStates): Unit = 
      val (_, content) = buildTab(safePath, extension, contentSections, currentSection = currentSection, states = states, currentState = state, omrMetadata = omrMetadata)
      panels.tabContent.updateTab(safePath, content)

    def switchFromResultView(resultView: ResultView): Unit = 
      val newState = updatedContentState
      val updatedStates = states.update(newState)
      switchFromState(ContentState.fromResultView(resultView, updatedStates), updatedStates)

    def switchSection(section: Section): Unit = 
      val (_, content) = buildTab(safePath, extension, contentSections, currentSection = section.name, omrMetadata = omrMetadata)
      panels.tabContent.updateTab(safePath, content)

    val rawToggleState = ToggleState(ResultView, "CSV", btn_primary_string, _ => switchFromResultView(Raw))
    val tableToggleState = ToggleState(ResultView, "Table", btn_primary_string, _ => switchFromResultView(Table))
    val plotToggleState = ToggleState(ResultView, "Plot", btn_primary_string, _ => switchFromResultView(Plot))
    val metadataToggleState = ToggleState(ResultView, "More", btn_primary_string, _ => switchFromResultView(Metadata))

    def viewIndex(state: ContentState) =
      state match
        case TableState(_,_) => 0
        case PlotState(_,_) => 1
        case RawState(_,_) => 2
        case _=> 3 

    val switchButton = exclusiveRadio(Seq(tableToggleState, plotToggleState, rawToggleState, metadataToggleState), btn_secondary_string, viewIndex(currentState))
    val refreshing: Var[Boolean] = Var(false)             

    val refreshButton =
      button(btn_purple, cls := "refreshOMR", glyph_refresh, marginLeft := "10px", onClick --> {_ => 
        refreshing.set(true)
        val plotContentState = updatedContentState

        api.omrContent(safePath).map: guiContent =>
          val (_, content) = OMRContent.buildTab(safePath, guiContent, contentStates = states.update(plotContentState), currentState = plotContentState)
          panels.tabContent.updateTab(safePath, content)
          refreshing.set(false)
      })
                   
    val sectionStates: Seq[ToggleState[Section]] =
      contentSections.map: cs =>
        val section = Section(cs.section)
        ToggleState(section, cs.section, s"btn ${btn_success_string}", _ => switchSection(section))

    lazy val sectionSwitchButton = exclusiveRadio(sectionStates, btn_secondary_string, 0)
          
    val content =
      div(display.flex, flexDirection.row,
        div(display.flex, flexDirection.column, width := "100%",
          div(display.flex, flexDirection.row, alignItems.center,
            div(
              child <-- refreshing.signal.map:
                case false => refreshButton
                case true => Waiter.waiter("#794985")
            ),
            currentState match
              case _: RawState => i(btn_purple, marginLeft := "20", cls :="btn bi-arrow-down", onClick --> setScrollToBottom )
              case _: TableState => i(btn_purple, marginLeft := "20", cls :="btn bi-arrow-down", onClick --> setScrollToBottom )
              case _ => div(),
            sectionStates.size match
              case 1 => div()
              case _ => sectionSwitchButton.element.amend(margin := "10", width := "150px")
            ,
            switchButton.element.amend(margin := "10", width := "150px", marginLeft := "30"),
          ),
          sectionView.view
        )
      )

//    timers.setTimeout(500) {
//       currentState match
//          case r: RawState if r.scrollDown =>
//            sectionView match
//              case EditorView(_, editor) => editor.scrollToBottom
//              case _=>
//          case ts: TableState if ts.scrollDown =>
//            sectionView match
//              case BasicView(view)=> setScrollToBottom(view)
//              case _=>
//          case _=>
//    }

    (tabData, content)



