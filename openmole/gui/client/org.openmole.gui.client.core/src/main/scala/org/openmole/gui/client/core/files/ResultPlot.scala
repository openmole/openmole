package org.openmole.gui.client.core.files

import org.openmole.gui.client.tool.plot.Plot.*
import org.openmole.plotlyjs.PlotlyImplicits.*
import org.openmole.gui.client.tool.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.tool.plot.{ParallelPlot, PlotSettings, Plotter, ScatterPlot, SplomPlot, Tools, XYPlot}
import org.openmole.gui.shared.data.SequenceData
import org.openmole.plotlyjs.*

import scala.scalajs.js.timers
import org.openmole.gui.client.tool.plot.Plot.SelectedPlot.{Method, N}

object ResultPlot:
  object ResultPlotState:
    case class One(indexes: Seq[Int] = Seq(0)) extends ResultPlotState
    case class Two(indexes: Seq[Int] = Seq(0, 1)) extends ResultPlotState
    case class N(indexes: Seq[Int] = Seq(0)) extends ResultPlotState
    case class Parallel(indexes: Seq[Int] = Seq(0)) extends ResultPlotState
    case class Method() extends ResultPlotState

  sealed trait ResultPlotState extends PlotContent.ContentState


import ResultPlot.*

class ResultPlot(plotData: ColumnData, methodPanel: Option[HtmlElement], val plotState: Var[ResultPlotState]):

  def plotSelection =
    val plotModeStates =
      Seq(
        ToggleState("1", "btn " + btn_danger_string, () => plotState.set(ResultPlotState.One())),
        ToggleState("2", "btn " + btn_danger_string, () => plotState.set(ResultPlotState.Two())),
        ToggleState("N", "btn " + btn_danger_string, () => plotState.set(ResultPlotState.N())),
        ToggleState("//", "btn " + btn_danger_string, () => plotState.set(ResultPlotState.Parallel()))
      ) ++ (
        if methodPanel.isDefined
        then Seq(ToggleState("M", "btn " + btn_danger_string, () => plotState.set(ResultPlotState.Method())))
        else Seq())

    val selected =
      plotState.now() match
        case _: ResultPlotState.One => 0
        case _: ResultPlotState.Two => 1
        case _: ResultPlotState.N => 2
        case _: ResultPlotState.Parallel => 3
        case _: ResultPlotState.Method => 4

    exclusiveRadio(plotModeStates, btn_secondary_string, selected)

  def headers = plotData.columns.map { _.header }

  def getPlot(state: ResultPlotState) =
    div(display.flex,
      state match
          case s: ResultPlotState.One =>
            val hIndex = s.indexes.head
            val header = plotData.columns(hIndex).header
            val columnContents = Column.contentToSeqOfSeq(plotData.columns(hIndex).content)
            XYPlot(columnContents, ("Records", header), PlotSettings())
          case s: ResultPlotState.Two =>
            val contents = s.indexes.map: sh =>
              Column.contentToSeqOfSeq(plotData.columns(sh).content).head
            ScatterPlot(contents.head, contents.last, (headers(s.indexes.head), headers(s.indexes.last)), PlotSettings())
          case s: ResultPlotState.N =>
            val contents = s.indexes.map: sh =>
              Column.contentToSeqOfSeq(plotData.columns(sh).content).head
            SplomPlot(contents, s.indexes.map(headers.apply), PlotSettings())
          case s: ResultPlotState.Parallel=>
            val contents = s.indexes.map: sh =>
              Column.contentToSeqOfSeq(plotData.columns(sh).content).head
            ParallelPlot(contents, s.indexes.map(headers.apply), PlotSettings())
          case _: ResultPlotState.Method =>
            methodPanel.getOrElse(div("No method visualization available"))
    )

  def axisCheckBoxes: Signal[Option[ExclusiveRadioButtons]] =

    def partitionColumns = plotData.columns.partition:
      _.content match
        case ArrayColumn(_) => true
        case _ => false

    plotState.signal.map:
      case s: ResultPlotState.One =>
        val axisToggleStates = headers.map(ah => ToggleState(ah, s"btn ${btn_danger_string}"))
        Some(exclusiveRadios(axisToggleStates, btn_secondary_string, s.indexes, SelectionSize.DefaultLength, todo = sel => plotState.set(s.copy(sel))))
      case s: ResultPlotState.Two  =>
        val (arrayColumn, scalarColumn) = partitionColumns
        val axisToggleStates =
          scalarColumn.map { _.header }.map: ah =>
            ToggleState(ah, s"btn ${btn_danger_string}")

        Some(exclusiveRadios(axisToggleStates, btn_secondary_string, s.indexes, SelectionSize.DefaultLength, todo = sel => plotState.set(s.copy(sel))))
      case s: ResultPlotState.Parallel =>
        val (arrayColumn, scalarColumn) = partitionColumns
        val axisToggleStates = scalarColumn.map { _.header }.map: ah =>
          ToggleState(ah, s"btn ${btn_danger_string}")

        Some(exclusiveRadios(axisToggleStates, btn_secondary_string, s.indexes, SelectionSize.Infinite, todo = sel => plotState.set(s.copy(sel))))
      case s: ResultPlotState.N =>
        val (arrayColumn, scalarColumn) = partitionColumns
        val axisToggleStates = scalarColumn.map {_.header}.map: ah =>
          ToggleState(ah, s"btn ${btn_danger_string}")

        Some(exclusiveRadios(axisToggleStates, btn_secondary_string, s.indexes, SelectionSize.Infinite, todo = sel => plotState.set(s.copy(sel))))
      case s: ResultPlotState.Method => None

  def fromColumnData =
    div(
      display.flex, flexDirection.column,
      div(
        display.flex, flexDirection.row,
        plotSelection.element.amend(margin := "10", height := "38"),
        child <-- axisCheckBoxes.signal.map:
          case Some(a) => a.element.amend(display.block, margin := "10")
          case None => emptyNode
      ),
      child <-- plotState.signal.map(getPlot)
    )
