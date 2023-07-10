package org.openmole.gui.client.core.files

import org.openmole.gui.client.tool.plot.Plot.*
import org.openmole.plotlyjs.PlotlyImplicits.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.tool.plot.{PlotSettings, Plotter, ScatterPlot, SplomPlot, Tools, XYPlot}
import org.openmole.gui.shared.data.SequenceData
import org.openmole.plotlyjs.*

object ResultPlot {

  def
  fromCSV(plotData: ColumnData) = {

    val headers = plotData.columns.map {
      _.header
    }
    val axisRadios: Var[ExclusiveRadioButtons[String]] = Var(exclusiveRadios[String](Seq(), "", Seq()))
    val plot: Var[HtmlElement] = Var(div())

    def doPlot(numberOfColumToBePlotted: NumberOfColumToBePlotted) = {
      val axisSelected = axisRadios.now().selected.now().map(_.t)
      numberOfColumToBePlotted match {
        case OneColumn =>
          val hIndex = headers.indexOf(axisSelected.head)
          val header = plotData.columns(hIndex).header
          val columnContents = Column.contentToSeqOfSeq(plotData.columns(hIndex).content)
          plot.set(XYPlot(columnContents, ("Records", header), PlotSettings())) // case Array)
        case TwoColumn =>
          val contents = axisSelected.map { sh =>
            Column.contentToSeqOfSeq(plotData.columns(headers.indexOf(sh)).content).head
          }
          plot.set(ScatterPlot(contents.head, contents.last, (axisSelected.head, axisSelected.last), PlotSettings()))
        case NColumn =>
          val contents = axisSelected.map { sh =>
            Column.contentToSeqOfSeq(plotData.columns(headers.indexOf(sh)).content).head
          }
          plot.set(SplomPlot(contents, axisSelected, PlotSettings()))
      }
    }


    // Axis selection
    // 1- only one selection ( 1 column button is set)
    //    - case array: plot n times indexes x selection values in XY mode
    //    - case scalar: plot indexes x selection values in XY mode
    // 2- 2 selections ( 2 column button is set)
    //    - arrays are not proposed
    //    - scalars: selection value 1 x selection value 2 in Scatter mode
    // 3- 'N column' selection
    //    - arrays are not proposed -> Splom for all selection values
    def axisCheckBoxes(numberOfColumToBePlotted: NumberOfColumToBePlotted) = {

      val allHeaders = plotData.columns.map {
        _.header
      }
      val (arrayColumn, scalarColumn) = plotData.columns.partition {
        _.content match {
          case ArrayColumn(_) => true
          case _ => false
        }
      }

      val (availableHeaders, initialIndexes) = numberOfColumToBePlotted match {
        case OneColumn => (allHeaders, Seq(0))
        case TwoColumn | NColumn => (scalarColumn.map {
          _.header
        }, Seq(0, 1))
      }

      lazy val todo: String => Unit = (h: String) => {
        doPlot(numberOfColumToBePlotted)
      }

      lazy val axisToggleStates = availableHeaders.map { ah => ToggleState[String](ah, ah, s"btn ${btn_danger_string}", todo) }

      val selectionMode = numberOfColumToBePlotted match {
        case NColumn => SelectionSize.Infinite
        case _ => SelectionSize.DefaultLength
      }

      axisRadios.set(exclusiveRadios(axisToggleStates, btn_secondary_string, initialIndexes, selectionMode))
    }

    val oneTwoNRadio = {
      val plotModeStates = Seq(OneColumn, TwoColumn, NColumn).zip(Seq("1", "2", "N")).map { case (pm, name) =>
        ToggleState(pm, name, "btn " + btn_danger_string, pm => {
          axisCheckBoxes(pm)
          doPlot(pm)
        })
      }
      exclusiveRadio[NumberOfColumToBePlotted](plotModeStates, btn_secondary_string, 0)
    }

    val plotObserver = Observer[(HtmlElement, Seq[String])] { case (p, hs) =>
      oneTwoNRadio.selected.now().map(_.t).head match {
        case NColumn => Plotly.relayout(p.ref, splomLayout)
        case _ => Plotly.relayout(p.ref, baseLayout(hs.headOption.getOrElse(""), hs.lastOption.getOrElse("Records")))
      }
    }

    div(
      child <-- oneTwoNRadio.selected.signal.combineWith(axisRadios.signal).map { (oneTwoNSelected, aRadio) =>

        div(display.flex, flexDirection.column,
          div(display.flex, flexDirection.row,
            oneTwoNRadio.element.amend(margin := "10", height := "38"),
            aRadio.element.amend(display.block, margin := "10")
          ),
          child <-- plot.signal,
          plot.signal.combineWith(aRadio.selected.signal.map(_.map(_.t))) --> plotObserver
        )
      }
    )
  }


}
